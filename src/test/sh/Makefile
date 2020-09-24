PROJECT := HarcHardware
JAVA := java
MYDIR := $(dir $(firstword $(MAKEFILE_LIST)))
TOP := $(realpath $(MYDIR)../../..)
COMMON := $(TOP)/common

VERSION := $(shell xsltproc $(COMMON)/xslt/extract_project_version.xsl $(TOP)/pom.xml)
JAR := $(TOP)/target/$(PROJECT)-$(VERSION)-jar-with-dependencies.jar
MAIN := $(JAVA) -jar $(JAR)

all: help version globalcache.test devslashlirc.test irtrans.test audio.test irtoy.test girs.test commandfusion.test irwidget.test

help: $(JAR)
	$(MAIN) --help

version: $(JAR)
	@echo $(VERSION)
	$(MAIN) version

$(JAR):
	(cd $(TOP); make)

clean:
	(cd $(TOP) ; make clean)
	rm -f *.test

devslashlirc.test: $(JAR)
	$(MAIN) --devslashlirc version
	$(MAIN) --devslashlirc transmit -p nec1 -n D=122,F=29
# Presently repeat not recognized-
	$(MAIN) --devslashlirc transmit --count 10 -p nec1 -n D=122,F=27
	@echo Now shoot an IR signal at your /dev/lirc device
	$(MAIN) --devslashlirc receive -r -d
	touch $@

girs.test: $(JAR)
	$(MAIN) --girs version
	$(MAIN) --girs transmit -p nec1 -n D=122,F=29
	$(MAIN) --girs transmit --count 10 -p nec1 -n D=122,F=27
	@echo Now shoot an IR signal at your /dev/arduino device. Wait for yellow led first.
	$(MAIN) --girs receive -r -d
	@echo Now shoot an IR signal at your /dev/arduino device. Wait for yellow led first.
	$(MAIN) --girs capture -r -d
	touch $@

irtoy.test: $(JAR)
	$(MAIN) --irtoy version
	$(MAIN) --irtoy transmit -p nec1 -n D=122,F=29
	$(MAIN) --irtoy transmit --count 10 -p nec1 -n D=122,F=27
#	@echo Now shoot an IR signal at your /dev/irtoy device.
#	$(MAIN) --irtoy receive -r -d
	@echo Now shoot an IR signal at your /dev/irtoy device.
	$(MAIN) --irtoy capture -r -d
	touch $@

globalcache.test: globalcache_send globalcache_capture

globalcache_send: $(JAR)
	$(MAIN) --globalcache version
	$(MAIN) --globalcache transmit -n D=1,F=36 -# 3 --protocol sony12
	touch $@

globalcache_capture: $(JAR)
	@echo Now shoot a signal at GlobalCache iTach Flex
	$(MAIN) --globalcache --ip itachflex capture -r -d
	touch $@

commandfusion.test: $(JAR)
	$(MAIN) --commandfusion version
	$(MAIN) --commandfusion transmit --count 10 -p nec1 -n D=122,F=27
	@echo Now shoot an IR signal at your /dev/ftdi device.
	$(MAIN) --commandfusion capture -r -d
	touch $@

irtrans.test: $(JAR)
	$(MAIN) --irtrans --ip irtrans version
	$(MAIN) --irtrans --ip irtrans -v transmit --count 3 -p sony12 -n D=1,F=21
	touch $@

audio.test: $(JAR)
	$(MAIN) --audio version
	$(MAIN) --audio -v transmit --count 10 -p nec1 -n D=122,F=27
	touch $@

irwidget.test: $(JAR)
	$(MAIN) --irwidget version
	@echo Now shoot an IR signal at your /dev/ftdi device.
	$(MAIN) --irwidget capture -r -d
	touch $@

irremote.test: $(JAR)
	@echo Now shoot an IR signal at your IR remote device.
	$(MAIN) --irremote receive -r -d
	touch $@

girs_tcp.test: $(JAR)
	ping -c 1 arduino
	$(MAIN) --class GirsClient --ip arduino --port 33333 version
	$(MAIN) --class GirsClient --ip arduino transmit -n D=1,F=36 -# 3 --protocol sony12
	@echo Now shoot an IR signal at your Arduino. Wait for yellow led first.
	$(MAIN) --class GirsClient --ip arduino receive -r -d
	@echo Now shoot an IR signal at your Arduino. Wait for yellow led first.
	$(MAIN) --class GirsClient --ip arduino capture -r -d
	touch $@
