#!/bin/bash

set -o errexit -o nounset -o pipefail

paths=(
    core/src/main/java
    core/src/test/java
    contactsbackup/src/androidTest/java
    contactsbackup/src/main/java
    contactsbackup/src/test/java
    storage/lib/src/androidTest/java
    storage/lib/src/main/java
    storage/lib/src/test/java
)

git mv storage/lib/schemas/org.calyxos.backup.storage.db.Db storage/lib/schemas/app.grapheneos.backup.storage.db.Db
git mv contactsbackup/default-permissions_org.calyxos.backup.contacts.xml contactsbackup/default-permissions_app.grapheneos.backup.contacts.xml

for path in ${paths[@]}; do
    git mv $path/org/calyxos/ $path/org/grapheneos/
    git mv $path/org/ $path/app/
done

find app core contactsbackup storage .cirrus.yml -type f -exec sed -i 's/org.calyxos/app.grapheneos/g' {} +
