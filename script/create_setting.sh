#!/usr/bin/env bash
# build libs only, only required in dev stage, after there exists changes in libs


MAVEN_SETTING_PATH=${1:-"settings.xml"}
MAVEN_SETTINGS=${ACI_VAR_MAVEN_SETTINGS:-$2}

if [ -f "$MAVEN_SETTING_PATH" ]; then
    echo "settings file already exists. ：$MAVEN_SETTING_PATH"
else
    echo "settings file does not exist, creating：$MAVEN_SETTING_PATH"
    RAW_SETTINGS=$(echo "$MAVEN_SETTINGS" | sed -e 's/&lt;/</g' -e 's/&gt;/>/g' -e 's/&quot;/"/g' -e 's/&amp;/&/g')
    echo "$RAW_SETTINGS" >> "$MAVEN_SETTING_PATH"
    echo "maven_settings:$RAW_SETTINGS"
fi
echo "create maven settings succeed"
exit 0
