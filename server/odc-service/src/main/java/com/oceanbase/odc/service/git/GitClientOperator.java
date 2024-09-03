/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.service.git;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.git.model.FileChangeType;
import com.oceanbase.odc.service.git.model.GitDiff;
import com.oceanbase.odc.service.git.model.GitStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/7/30
 */
@Slf4j
public class GitClientOperator implements Closeable {
    private static final byte[] EMPTY_BYTES = new byte[0];

    private final Git git;

    public GitClientOperator(String gitDir) throws IOException {
        this(new File(gitDir));
    }

    public GitClientOperator(File gitDir) throws IOException {
        this(Git.open(gitDir));
    }

    public GitClientOperator(Git git) {
        this.git = git;
    }

    public static GitClientOperator cloneRepo(String cloneUrl, File dest, String email, String token)
            throws IOException, GitAPIException {
        if (!dest.exists()) {
            FileUtils.forceMkdir(dest);
        }
        Verify.verify(dest.isDirectory(), "clone destination is not a directory");
        Git.cloneRepository()
                .setURI(cloneUrl)
                .setDirectory(dest)
                .setProgressMonitor(new TextProgressMonitor(new LogProgressWriter()))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(email, token))
                .call();
        return new GitClientOperator(dest);
    }

    public Set<String> listBranches() throws GitAPIException {
        return git.branchList().setListMode(ListMode.ALL).call()
                .stream()
                .map(r -> {
                    String name = r.getName();
                    if (name.startsWith("refs/heads/")) {
                        return name.substring("refs/heads/".length());
                    }
                    return name.substring("refs/remotes/origin/".length());
                })
                .collect(Collectors.toSet());
    }

    public String currentBranch() throws IOException {
        return git.getRepository().getBranch();
    }

    public String createBranch(String branch) throws GitAPIException {
        return git.branchCreate().setName(branch).call().getName();
    }

    public String checkout(String branch) throws GitAPIException {
        Ref newRef = git.checkout()
                .setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
                .setName(branch)
                .setProgressMonitor(new TextProgressMonitor(new LogProgressWriter()))
                .call();
        return newRef.getName();
    }

    public PullResult pull(String branch) throws GitAPIException, IOException {
        return git.pull()
                .setRemoteBranchName(branch)
                .setProgressMonitor(new TextProgressMonitor(new LogProgressWriter()))
                .call();
    }

    /**
     * @param path relative path
     */
    public void add(String path) throws GitAPIException {
        git.add().addFilepattern(path).call();
    }

    public void commit(String message, String email) throws GitAPIException {
        git.commit()
                .setMessage(message)
                .setAuthor(email, email)
                .call();
    }

    public List<RemoteRefUpdate.Status> push(String email, String token) throws GitAPIException {
        Iterable<PushResult> gitPushResult = git.push()
                .setRemote("origin")
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(email, token))
                .setProgressMonitor(new TextProgressMonitor(new LogProgressWriter()))
                .call();
        List<PushResult> results = IteratorUtils.toList(gitPushResult.iterator());
        Verify.singleton(results, String.format("push result is not a singleton, detail:[%s]",
                results.stream().map(PushResult::getRemoteUpdates).collect(Collectors.toList()).toString()));

        return results.get(0).getRemoteUpdates()
                .stream()
                .map(RemoteRefUpdate::getStatus)
                .collect(Collectors.toList());
    }

    /**
     * {@link DiffFormatter#scan} method could not detect conflicted files, so we use
     * {@link org.eclipse.jgit.api.DiffCommand} as complement
     */
    public List<GitDiff> diff() throws IOException, GitAPIException {
        List<GitDiff> changes = new ArrayList<>();

        Repository repository = git.getRepository();
        FileTreeIterator workTreeIterator = new FileTreeIterator(repository);
        CanonicalTreeParser headTreeParser = prepareTreeParser("HEAD");

        try (DiffFormatter diffFormatter = new DiffFormatter(new EmptyOutputStream())) {
            diffFormatter.setRepository(repository);
            // if this is not set, the renamed files would be represented as deleted files and added files
            diffFormatter.setDetectRenames(true);

            List<DiffEntry> diffs = diffFormatter.scan(headTreeParser, workTreeIterator);
            for (DiffEntry entry : diffs) {
                GitDiff diff = new GitDiff();
                switch (entry.getChangeType()) {
                    case ADD:
                    case COPY:
                        diff.setState(FileChangeType.A);
                        diff.setNewPath(entry.getNewPath());
                        break;
                    case DELETE:
                        diff.setState(FileChangeType.D);
                        diff.setOldPath(entry.getOldPath());
                        break;
                    case RENAME:
                        diff.setState(FileChangeType.R);
                        diff.setOldPath(entry.getOldPath());
                        diff.setNewPath(entry.getNewPath());
                    case MODIFY:
                        diff.setState(FileChangeType.M);
                        diff.setOldPath(entry.getOldPath());
                        diff.setNewPath(entry.getNewPath());
                    default:
                        break;
                }
                changes.add(diff);
            }
        }
        Set<String> conflicts = git.status().call().getConflicting();
        for (GitDiff diff : changes) {
            if (conflicts.contains(diff.getNewPath())) {
                diff.setState(FileChangeType.C);
            }
        }
        return changes;
    }

    public GitDiff diffContent(GitDiff diff) throws IOException, GitAPIException {
        Repository repository = git.getRepository();
        RawText oldText;
        RawText newText;
        String oldPath = diff.getOldPath();
        String newPath = repository.getDirectory().getParent() + File.separator + diff.getNewPath();
        switch (diff.getState()) {
            case A:
                oldText = new RawText(EMPTY_BYTES);
                newText = new RawText(FileUtils.readFileToByteArray(new File(newPath)));
                break;
            case D:
                oldText = new RawText(getFileContent("HEAD", oldPath).getBytes());
                newText = new RawText(EMPTY_BYTES);
                break;
            case R:
            case M:
            case C:
            default:
                newText = new RawText(FileUtils.readFileToByteArray(new File(newPath)));
                oldText = new RawText(getFileContent("HEAD", oldPath).getBytes());
                break;
        }

        DiffAlgorithm diffAlgorithm = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM);
        EditList editList = diffAlgorithm.diff(RawTextComparator.DEFAULT, oldText, newText);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            DiffFormatter diffFormatter = new DiffFormatter(out);
            diffFormatter.setRepository(repository);
            diffFormatter.format(editList, oldText, newText);
            diff.setDiff(out.toString());
        }
        return diff;
    }

    public GitStatus status() throws GitAPIException, IOException {
        String branch = currentBranch();
        String remoteBranch = "origin/" + branch;
        Iterable<RevCommit> commits = git.log()
                .addRange(git.getRepository().resolve(branch), git.getRepository().resolve(remoteBranch))
                .call();
        int commitsBehind = 0;
        for (RevCommit commit : commits) {
            commitsBehind++;
        }

        GitStatus gitStatus = new GitStatus();
        gitStatus.setBranch(branch);
        gitStatus.setCommitsBehindRemote(commitsBehind);
        gitStatus.setChanges(diff());
        return gitStatus;
    }

    public String lastCommitId() throws GitAPIException {
        return git.log().setMaxCount(1).call().iterator().next().getName();
    }

    public void resetHardToHead() throws GitAPIException {
        resetHard("HEAD");
    }

    public void resetHard(String version) throws GitAPIException {
        git.reset()
                .setMode(ResetType.HARD)
                .setRef(version)
                .call();
    }

    public ByteArrayOutputStream getDiffForPatch() throws GitAPIException {
        List<DiffEntry> diffs = git.diff().setCached(true).call();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DiffFormatter formatter = new DiffFormatter(out)) {
            formatter.setRepository(git.getRepository());
            diffs.forEach(d -> {
                try {
                    formatter.format(d);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return out;
    }

    public void applyDiff(InputStream in) throws GitAPIException {
        git.apply().setPatch(in).call();
    }

    public File getRepoDir() {
        return git.getRepository().getDirectory().getParentFile();
    }

    @Override
    public void close() {
        git.close();
    }

    private CanonicalTreeParser prepareTreeParser(String ref) throws IOException {
        Repository repository = git.getRepository();
        try (RevWalk walk = new RevWalk(repository)) {
            ObjectId commitId = repository.resolve(ref);
            RevCommit commit = walk.parseCommit(commitId);
            RevTree tree = commit.getTree();

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();
            return treeParser;
        }
    }

    private String getFileContent(String ref, String filePath) throws IOException {
        Repository repository = git.getRepository();
        try (RevWalk revWalk = new RevWalk(repository)) {
            ObjectId lastCommit = repository.resolve(ref);
            RevCommit commit = revWalk.parseCommit(lastCommit);
            try (TreeWalk treeWalk = TreeWalk.forPath(repository, filePath, commit.getTree())) {
                if (treeWalk != null) {
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    repository.open(objectId).copyTo(out);
                    return out.toString();
                }
            }
        }
        throw new FileNotFoundException();
    }

    private static class LogProgressWriter extends Writer {
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            log.info(new String(cbuf, off, len));
        }

        @Override
        public void flush() throws IOException {}

        @Override
        public void close() throws IOException {}
    }

    private static class EmptyOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {}
    }

}
