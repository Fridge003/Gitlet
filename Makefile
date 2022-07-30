# This makefile is defined to give you the following targets:
#
#    default: The default target: Compiles the program in package db61b.
#    clean: Remove regeneratable files (such as .class files) produced by
#           other targets and Emacs backup files.
#
# In other words, type 'make' to compile everything, and 'make clean' to clean things up.
# 

# Name of package containing main procedure 
PACKAGE = gitlet

RMAKE = "$(MAKE)"

# Targets that don't correspond to files, but are to be treated as commands.
.PHONY: default clean

default:
	$(RMAKE) -C $(PACKAGE) default

# 'make clean' will clean up stuff you can reconstruct.
clean:
	$(RM) *~
	$(RMAKE) -C $(PACKAGE) clean


