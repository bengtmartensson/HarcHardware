PROJECT := HarcHardware
JAVA := java
MYDIR := $(dir $(firstword $(MAKEFILE_LIST)))
TOP := $(realpath $(MYDIR)../../..)
COMMON := $(TOP)/common

VERSION := $(shell xsltproc $(COMMON)/xslt/extract_project_version.xsl $(TOP)/pom.xml)
JAR := $(TOP)/target/$(PROJECT)-$(VERSION)-jar-with-dependencies.jar
MAIN := $(JAVA) -jar $(JAR)

all: help.test version.test globalcache.test devslashlirc.test irtrans.test audio.test irtoy.test girs.test commandfusion.test irwidget.test

help.test: $(JAR)
	$(MAIN) --help
	touch $@

version.test: $(JAR)
	@echo $(VERSION)
	$(MAIN) version
	touch $@

$(JAR):
	(cd $(TOP); make)

clean:
	(cd $(TOP) ; make clean)
	rm -f *.test

devslashlirc.test: $(JAR)
	@echo Now testing the /dev/lirc device
	$(MAIN) --devslashlirc version
	$(MAIN) --devslashlirc --begin 10000 receive -r -d
	$(MAIN) --devslashlirc transmit -p nec1 -n D=122,F=29
# Presently repeat not recognized-
	$(MAIN) --devslashlirc transmit --count 10 -p nec1 -n D=122,F=27
	@echo Now shoot an IR signal at your /dev/lirc device
	touch $@

girs.test: $(JAR)
	@echo Now testing the Arduino device
	$(MAIN) --girs version
	$(MAIN) --girs transmit -p nec1 -n D=122,F=29
	$(MAIN) --girs transmit --count 10 -p nec1 -n D=122,F=27
	@echo Now shoot an IR signal at your /dev/arduino device. Wait for yellow led first.
	$(MAIN) --girs receive -r -d
	@echo Now shoot an IR signal at your /dev/arduino device. Wait for yellow led first.
	$(MAIN) --girs capture -r -d
	touch $@

irtoy.test: $(JAR)
	@echo Now testing the IrToy device
	$(MAIN) --irtoy version
	$(MAIN) --irtoy transmit -p nec1 -n D=122,F=29
	$(MAIN) --irtoy transmit --count 10 -p nec1 -n D=122,F=27
#	@echo Now shoot an IR signal at your /dev/irtoy device.
#	$(MAIN) --irtoy receive -r -d
	@echo Now shoot an IR signal at your /dev/irtoy device.
	$(MAIN) --irtoy capture -r -d
	touch $@

globalcache.test: globalcache_send.test globalcache_capture.test

globalcache_send.test: $(JAR)
	@echo Now testing the GlobalCache device at globalcache
	$(MAIN) --globalcache version
	$(MAIN) --globalcache transmit -n D=1,F=36 -# 3 --protocol sony12
	touch $@

globalcache_capture.test: $(JAR)
	@echo Now shoot a signal at GlobalCache iTach Flex
	$(MAIN) --globalcache --ip itachflex --begin 10000 capture -r -d
	touch $@

commandfusion.test: $(JAR)
	@echo Now testing the CommandFusion device
	$(MAIN) --commandfusion version
	$(MAIN) --commandfusion transmit --count 10 -p nec1 -n D=122,F=27
	@echo Now shoot an IR signal at your /dev/ftdi device.
	$(MAIN) --commandfusion capture -r -d
	touch $@

irtrans.test: $(JAR)
	@echo Now testing the IrTrans device
	$(MAIN) --irtrans --ip irtrans version
	$(MAIN) --irtrans --ip irtrans -v transmit --count 3 -p sony12 -n D=1,F=21
	touch $@

audio.test: $(JAR)
	@echo Now testing the Audio device
	$(MAIN) --audio version
	$(MAIN) --audio -v transmit --count 10 -p nec1 -n D=122,F=27
	touch $@

irwidget.test: $(JAR)
	@echo Now testing the IrWidget device
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
