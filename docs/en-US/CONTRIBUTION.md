# Contributing to OceanBase Developer Center

👀 Wow! Glad you opened this document.

If you are interested in participating in the OceanBase Developer Center project,
whether it is to make comments and suggestions or even criticism, or to report bugs or contribute code,
we are very welcome 👏👏.

> These are mostly guidelines, not rules. if you have better suggestions, you can also directly propose a PR to modify this document.

#### Table Of Contents

- [Code of Conduct](#code-of-conduct)
- [Question or Discussion](#questions-or-discussion)
- [Before Contribution](#before-contribution)
- [Reporting Bugs](#reporting-bugs)
- [Pull Requests](#pull-requests)

## Code of Conduct

This project and everyone participating in it is governed by the [Code of Conduct](../../CODE_OF_CONDUCT.md). By participating, you are
expected to uphold this code. Please report unacceptable behavior to [yizhou.xw@oceanbase.com](mailto:yizhou.xw@oceanbase.com).

## Questions or Discussion

> **Note:** Please don't file an issue to ask a question. You'll get faster results by using the resources below.

* If you encounter a problem while using the product, please check [FAQ Document](https://github.com/oceanbase/odc-doc/blob/V4.2.0/zh-CN/1300.troubleshooting/100.general-troubleshooting.md) first, you may get answer from exists questions.
* For comments and suggestions on products, please start a discussion through [Discussions](https://github.com/oceanbase/odc/discussions).

## Before Contribution

### Git repo structure

The directory structure of this repository is described below.

```shell
├── builds                 Build environment configuration
│   └── code-style         Code formatting configuration
├── client                 Front-end code
├── distribution           Distribution configuration
│   ├── docker
│   ├── jar
│   ├── plugins
│   └── rpm
├── docs                   Documentation
│   ├── en-US      
│   └── zh-CN
├── import                 External components imported via executable files
├── libs                   Basic libraries used by ODC, can also be used by other projects
│   ├── db-browser
│   └── ob-sql-parser
├── script                 Common scripts used for setting up development environment, building programs, starting services, etc.
├── server                 Back-end code
│   ├── 3rd-party          Third-party packages imported via source code
│   │   └── Libinjection   Source: https://github.com/jeonglee/Libinjection Since the source repository does not publish a jar to the Maven repository, only source code can be referenced
│   ├── integration-test   Integration tests
│   ├── odc-common         Utility library
│   ├── odc-core           Core module for technical framework and business independent functionality
│   ├── odc-server         Server module, includes web framework and request routing configuration
│   ├── odc-service        Service module, includes business functionality implementation
│   ├── odc-test           Test utility library, made into a separate module for easy reuse by other modules
│   ├── plugins            Interface definition and specific implementations for plugins
│   │   ├── connect-plugin-api
│   │   ├── connect-plugin-mysql
│   │   ├── connect-plugin-ob-mysql
│   │   ├── connect-plugin-ob-oracle
│   │   ├── sample-plugin
│   │   ├── sample-plugin-api
│   │   ├── schema-plugin-api
│   │   ├── schema-plugin-mysql
│   │   ├── schema-plugin-ob-mysql
│   │   ├── schema-plugin-ob-oracle
│   │   └── sample-plugin-odp-sharding-ob-mysql
│   ├──  starters           Start-up configurations for different distribution versions
│   │   ├── desktop-starter Desktop version start-up configuration
│   │   └── web-starter     Web version start-up configuration
│   └── test-script         Common test scripts used for executing and analyzing test cases.
```

### Learn Developer Guide

Please read [Developer Guide](./DEVELOPER_GUIDE.md) to learn how to set up the development environment, build, launch, and debug.

## Reporting Bugs

> **Note:** If you find a **Closed** issue that seems like it is the same thing that you're experiencing,
> open a new issue and include a link to the original issue in the body of your new one.

### Confirm Bug Firstly

1. Check [FAQ](https://github.com/oceanbase/odc-doc/blob/V4.2.0/zh-CN/1300.troubleshooting/100.general-troubleshooting.md), confirm it is by design or not.
2. Do some analyzing or debugging, try to figure out the root cause.
3. Determine which repository the problem should be report in.
4. Check exists issue, confirm it's a new bug.

### Submit A Good Bug Report

Providing accurate and complete scene description information and reproduction steps is important for defect communication.

### Problem phenomenon based title

An accurate title is the first step in understanding the flaws. Many times we overlook this, and some bad practices may include:

- Based on the cause rather than the phenomenon.
- Overly simplistic.

We recommend

- The title should be in English.
- The title of the defect should be based on the problem phenomenon and be as accurate as possible.
- The title can contain the name of the module, version, etc., surrounded by square brackets, which is convenient to quickly determine the scope of the problem.

Good examples

```text
Windows App crashed while startup, got 'chunk not found' error in odc.log
[PL] PL execute got 'timeout of 3000000ms exceeded' error
[Table][OceanBase Oracle] NullPointerException while view temporary table
```

Bad examples

```text
Crashed (too simple, should be detailed)
Remove heartbeat mechanism （it is solution, not problem）
```

### Describe the bug correctly and completely

Please use the following template to file an bug.

```markdown
[Problem Overview]

[Environment and Version]

- ODC version:
- Database version:
- OS version:
- Database configuration information:

[Reproduction steps]
Complete steps for problem reproduction.

[Related Information]
Including error screenshots, program logs and other information,
if the log content is large, please use the attachment to upload.
```

## Pull Requests

Pull Request (PR) is a necessary process for merging personal code into iteration branches, iteration branches, and trunk branches during ODC development.

An effective and efficient PR process is not easy, and it is hoped that the standard described in this article can help achieve the following effects:

- The review process of PRs can be more clear and concise, with reviewers easily able to understand the content of the PR.
- PRs can form clear associations with the iteration tasks (mainly requirements and defects).
- Quality engineers can understand the scope of code changes through PR descriptions, making it easy to identify the corresponding test coverage.
- The atmosphere of the PR discussion process is serious and lively, promoting mutual learning and growth.
- The commit history that is eventually merged into the iteration branch or main branch is very readable and can generate CHANGELOGs automatically.

### Before submit PR

Each PR, whether for fixing defects or implementing new features, needs to be associated with a github issue.

- Firstly, please confirm whether the relevant issue has been created. If not, create the issue first.
- Before deciding to handle a certain issue, please communicate with the project members to confirm the ownership of the issue, and assign it to yourself.
- For defect fixing, make sure that all existing test cases can pass and that the related defects have been verified by self-testing.
- For feature implementation, ensure that the requirement and summary design documents have been reviewed and approved.

### Title for PR

There exists 3 scenarios while Pull Request

- fix a bug
- implement a new feature
- merge from branch to another

#### Fix bug/new feature

PR title for fix bug/new feature should follow Conventional Commits [git commit template](https://www.conventionalcommits.org/en/v1.0.0/#summary) 。

Title format `<type>[optional scope]: <description>`，
Notice the `<`,`>` char in `<type>` is not requires，
Optional type values: feat/fix/refactor/style/ci/docs/test/...

> You can install IDEA plugin to assist in generating git commit messages that meet the standards.
> Plugin [git-commit-template](https://plugins.jetbrains.com/plugin/9861-git-commit-template) is recommended.

Good examples

```text
fix(table): fix get partition failed if connect to public aliyun address
feat(security): add parameter validation to protect from sql injection
```

Bad examples

```text
table constraint
4.2.0 delimiter fix someusername
feat<table>: support column rename
```

#### Merge from branch to another

Title template `merge  <source_branch> into <targer_branch>` 。

examples

- merge 3.1.x_dev into master
- merge master into 3.2.x_dev
- merge 3.1.1_feature into 3.2.x_dev

### Bug fixing description template

Please reference this template for bug fixing.

```markdown
#### Defect Description

<< Describe the defect, including its cause, the versions it affects, and other relevant information. >>

#### Fix Solution

<< Provide an overview of the solution to fix the defect. If the fix involves changes to other modules, give an explanation here. >>

#### Testing Instructions

<< List the test cases that have been performed, the testing methods used, and suggest scenarios to ensure complete coverage. >>

#### Workaround

<< If there is a way to work around the defect, provide instructions here. If there is no workaround, state "N/A". >>

#### Other Information

<< Provide any other information that may be relevant. If there is no additional information, write "N/A". >>
```

### Feature implementation description template

Please reference this template for feature implementation.

```markdown
#### Implementation Overview

<< A paragraph or multiple paragraphs describing the details of the PR, including links to any relevant design documents for complex implementations. >>

#### Related Modules

<< The related modules involved in the PR. QA should focus on this section to determine the scope of the testing coverage. >>

#### Testing Suggestions

<< Notes on testing considerations and suggested testing scenarios. >>

#### TODO

<< Any outstanding issues that still need to be resolved. >>
```

### Efficient PR Considerations

#### Scale and Scope

PR requires review from other team members, and the size of each PR should not be too large.

- The review time for a single PR should be controlled within 2 hours.
- The number of lines of code in a PR is not mandatory, but it should not exceed 500 lines.
- For bug fixes, unrelated bug fixes should not be merged into the same PR.
    - Even if some bugs are fixed by modifying only one line of code, they should be submitted in separate PRs.
    - If the fixes for multiple bugs are related and cannot be separated technically or separating them will make it more difficult to understand, they can be merged into one PR.

#### Polite and Focused

The PR discussion process can bring different benefits, not just a fault-finding process, but also a learning and communication process.

We encourage:

- Praising good code.
- Asking questions about code that is not easy to understand.
- Discussing better solutions to solve problems.
- Sharing better programming practices.
- Exploring better naming conventions.
- Responding to PR opinions in a timely manner.
- ...

We do not encourage:

- Focusing too much on code formatting, such as blank lines, indentation, line breaks, etc. This part should be handled by automatic formatting.
- Approving PRs without careful review.
- ...

#### Collaboration and Feedback

PR is a collaborative process that requires timely response to feedback from reviewers.

- Improve responsiveness: After submitting a PR, you should respond to reviewer feedback as soon as possible to avoid long waiting times.
- Communication and discussion: If there are any questions or issues that need to be discussed, timely communication and discussion should be carried out to avoid misunderstandings or unnecessary delays.
- Problem-solving: If the reviewer raises any issues or improvement suggestions, you should actively solve them to ensure the quality and effectiveness of the PR.
