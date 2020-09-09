#!/bin/bash -ex

(
	cd "${0%/*}"
	if [ ! -d target/classes/javascript ]; then
		mvn -q dependency:unpack@javascript-unpack
	fi
)


search=( $(find "${0%/*}/src/main/resources/javascript" -type d -not -path "*/.*") "${0%/*}/target/dependency/javascript" )

exec dbc-jsshell --search "${search[*]/#/file:}" "$@"

