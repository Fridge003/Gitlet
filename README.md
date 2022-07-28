# Readme

"Gitlet" is a toy version of git designed by Joseph Moghadam from UC Berkeley. It contains some mostly used functions in git
such as init, add, commit, branch, checkout, etc.. This is also a project of UCB course CS61B: data structure. 

In this repository, I implemented my own version of "Gitlet" in Java, with the help of the skeleton codes & testing codes provided by UCB. 
For further details, please contact me through e-mail: eddiezhang@pku.edu.cn


## How to start Gitlet

First, clone this repository, and move to the root of your local repository in your shell (Gitlet is used through command line).
As you can see, there is a Makefile under the current path . 

First, input
```bash
make
```
the java files 
will be compiled under the instruction of Makefile. Then list the items of folder 'gitlet' in the current path, you will find a bunch of compiled java class files.
You can still stay here and regard the cloned repository as your working place, or you can copy the gitlet folder with compiled classes to 
another folder as working place, and move yourself to that folder. Anyway, before you start using gitlet, you should ensure
that there exists a folder called gitlet on your current directory, and the gitlet folder contains the compiled java .class files we need.

For any command supported in Gitlet, it follows the form:
```bash
java gitlet.Main ARGS  
```
where ARGS contains &lt;COMMAND&gt; &lt;OPERAND1&gt; &lt;OPERAND2&gt; ...


## Commands

### init


```bash
java gitlet.Main init
```

Creates a new Gitlet version-control system in the current directory. 
This system will automatically start with one commit: a commit that contains no files 
and has the commit message "initial commit". 
It will have a single branch: master, which initially points to this initial commit, 
and master will be the current branch. The timestamp for this initial commit will be 00:00:00 UTC,
Thursday, 1 January 1970 in whatever format you choose for dates. 


### add

```bash
java gitlet.Main add [file name]
```
Adds a copy of the file as it currently exists to the staging area. 
Staging an already-staged file overwrites the previous entry in the staging area with the new contents. 
If the current working version of the file is identical to the version in the current commit, remove it from the staging area if it is already there (as can happen when a file is changed, added, and then changed back to it’s original version). 
The file added will no longer be staged for removal (gitlet rm command), if it was at the time of the command.

In real git, multiple files may be added at once. However in gitlet, only one file may be added at a time.

### commit

```bash
java gitlet.Main commit [Message]
```

Saves a snapshot of tracked files in the current commit and staging area so they can be restored at a later time, 
creating a new commit. The commit is said to be tracking the saved files. 
By default, each commit’s snapshot of files will be exactly the same as its parent commit’s snapshot of files; 
it will keep versions of files exactly as they are, and not update them. A commit will only update the contents of files 
it is tracking that have been staged for addition at the time of commit, 
in which case the commit will now include the version of the file that was staged instead of the version it got from its parent. 

A commit will save and start tracking any files that were staged for addition but weren’t tracked by its parent. 
Also, files tracked in the current commit may be untracked in the new commit as a result being staged for removal by the rm command (below).


In real git, commits may have multiple parents (due to merging) and also have considerably more metadata. However in gitlet,
commits have only one parent, and metadata only include message and timestamp.


### remove

### status

### checkout

## Ideas of Design

### The design of .gitlet directory

<!--- Maybe I should mention the beautiful graph in the spec, and explain the data structure --->
