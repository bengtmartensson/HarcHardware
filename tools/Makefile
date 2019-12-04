# NOTE: This Makefile is not required to build the program, for which maven
# is used. Instead, it invokes the program for tests and for transforming the
# output, for example to the lirc.xml file.

MYDIR := $(dir $(firstword $(MAKEFILE_LIST)))
TOP := $(realpath $(MYDIR)..)

include $(MYDIR)/paths.mk

PROJECT_NAME := HarcHardware
PROJECT_NAME_LOWERCASE := $(shell echo $(PROJECT_NAME) | tr A-Z a-z)
EXTRACT_VERSION := $(TOP)/tools/extract_project_version.xsl
VERSION := $(shell $(XSLTPROC) $(EXTRACT_VERSION) HarcHardware/pom.xml)
PROJECT_JAR := target/$(PROJECT_NAME)-$(VERSION).jar
PROJECT_BIN := target/$(PROJECT_NAME)-$(VERSION)-bin.zip
GH_PAGES := $(TOP)/HarcHardware/gh-pages
ORIGINURL := $(shell git remote get-url origin)

default: $(PROJECT_JAR)

$(PROJECT_JAR) $(PROJECT_BIN):
	mvn install -Dmaven.test.skip=true

$(PROJECT_JAR)-test:
	mvn install -Dmaven.test.skip=false

release: push gh-pages tag deploy

version:
	@echo $(VERSION)

setversion:
	( cd HarcHardware ; mvn versions:set -DnewVersion=$(NEWVERSION) )
	git commit -S -m "Set version to $(NEWVERSION)" pom.xml

deploy:
	( cd HarcHardware ; mvn deploy -P release )

apidoc: HarcHardware/target/site/apidocs
	$(BROWSE) $</index.html

javadoc: HarcHardware/target/site/apidocs

HarcHardware/target/site/apidocs:
	(cd HarcHardware ; mvn javadoc:javadoc )

push:
	git push

gh-pages: HarcHardware/target/site/apidocs
	rm -rf $(GH_PAGES)
	git clone --depth 1 -b gh-pages ${ORIGINURL} ${GH_PAGES}
	( cd ${GH_PAGES} ; \
	cp -r ../HarcHardware/target/site/apidocs/* . ; \
	git add * ; \
	git commit -a -m "Update of API documentation" ; \
	git push )

tag:
	git checkout master
	git status
	git tag -s -a Version-$(VERSION) -m "Tagging Version-$(VERSION)"
	git push origin Version-$(VERSION)

clean:
	mvn clean
	rm -rf $(GH_PAGES)

.PHONY: clean $(PROJECT_JAR)-test release
