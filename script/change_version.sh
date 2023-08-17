#!/usr/bin/env bash
# See https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm
cd "$(git rev-parse --show-toplevel)" || exit 1

# in order to adapt all os, so we use sed print other new file and overwrite old file
function sed_file() {
    local replace_expression=$1
    local file_name=$2
    sed "${replace_expression}" "${file_name}" >"${file_name}.bak" && mv -f "${file_name}.bak" "${file_name}"
}

# for rpmbuild on Aone
function set_rpm_version() {
    local full_version="$1"
    local formal_version=$(echo "${full_version}" | awk -F - '{print $1}')
    echo "$formal_version" >distribution/odc-server-VER.txt
    return $?
}

function set_sqlconsole_submodule_branch() {
    local version="$1"
    local branch_name="dev-${version}"
    git config -f ".gitmodules" --replace-all submodule.client.branch "${branch_name}"
    local current_branch="$(git config -f .gitmodules --get submodule.client.branch)"
    echo "set sqlconsole submodule branch, current_branch=${current_branch}"
}

function main() {
    echo "change version..."
    case X$1 in
    Xset-version)
        local target_version="$2"
        if [ -z "${target_version}" ]; then
            echo "<target version> required while set-version"
            return 1
        fi
        local new_version="$2-SNAPSHOT"
        echo "set-version ${new_version}"
        mvn versions:set -DnewVersion="${new_version}"
        mvn versions:commit
        set_rpm_version "${new_version}"
        set_sqlconsole_submodule_branch "${target_version}"
        ;;
    Xset-release)
        local release_id="$2"
        if [ -z "${release_id}" ]; then
            release_id=$(date +%Y%m%d)
            echo "release_id not set, use current date ${release_id}"
        fi
        #e.g. 2.3.3-SNAPSHOT --> cut to 2.3.3
        local main_version="$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | cut -d - -f 1)"
        local new_version="${main_version}-${release_id}"
        echo "release $new_version"
        mvn versions:set -DnewVersion="${new_version}"
        mvn versions:commit
        set_rpm_version "${new_version}"
        ;;
    Xshow-version)
        mvn help:evaluate -Dexpression=project.version -q -DforceStdout
        ;;
    Xshow-main-version)
        mvn help:evaluate -Dexpression=project.version -q -DforceStdout | cut -d - -f 1
        ;;
    *)
        echo "Usage: change_version.sh set-version <target version> |set-release|show-version|show-main-version"
        echo "Examples:"
        echo "     change_version.sh set-version 2.3.3"
        echo "     change_version.sh set-release <release_id:'date +%Y%m%d' as default>"
        echo "     change_version.sh show-version"
        echo "     change_version.sh show-main-version"
        ;;
    esac
}

main $*
exit $?
