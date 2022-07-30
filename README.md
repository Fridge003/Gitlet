# Readme

"Gitlet" is a toy version of git designed by Joseph Moghadam from UC Berkeley. It contains some mostly used functions in git
such as init, add, commit, branch, checkout, etc.. This is also a project of UCB course CS61B: data structure. 

In this repository, I implemented my own version of "Gitlet" in Java, with the help of the skeleton codes & testing codes provided by UCB. 
Gitlet only supports local operations, so remote operations like "push" or "fetch" are not implemented in Gitlet. 
Gitlet doesn't support subdirectories, so please put all the files under the working directory and don't create any subdirectories.

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

Each commit has a log message associated with it (the [Message] argument) 
that describes the changes to the files in the commit. This is specified by the user. 
The entire message should take up only one entry in the array args that is passed to main. 
To include multiword messages, you’ll have to surround them in quotes.



In real git, commits may have multiple parents (due to merging) and also have considerably more metadata. However in gitlet,
commits have only one parent, and metadata only include message and timestamp.


### rm

```bash
java gitlet.Main rm [file name]
```
Unstage the file if it is currently staged for addition. 
If the file is tracked in the current commit, stage it for removal and remove the file from the working directory 
if the user has not already done so (do not remove it unless it is tracked in the current commit).

### checkout

There are three ways to use the checkout command:

```bash 
java gitlet.Main checkout -- [file name]
```
Takes the version of the file as it exists in the head commit and puts it in the working directory, 
overwriting the version of the file that’s already there if there is one. 
The new version of the file is not staged.

```bash 
java gitlet.Main checkout [commit id] -- [file name]
```
Takes the version of the file as it exists in the commit with the given id, 
and puts it in the working directory, overwriting the version of the file that’s already there if there is one. The new version of the file is not staged.

```bash 
java gitlet.Main checkout [branch name]
```
Takes all files in the commit at the head of the given branch, 
and puts them in the working directory, overwriting the versions of the files that are already there if they exist. 
Also, at the end of this command, the given branch will now be considered the current branch (HEAD). 
Any files that are tracked in the current branch but are not present in the checked-out branch are deleted. 
The staging area is cleared, unless the checked-out branch is the current branch


### branch
```bash
java gitlet.Main branch [branch name]
```
Creates a new branch with the given name, and points it at the current head commit.


### rm-branch
```bash
java gitlet.Main rm-branch [branch name]
```
Deletes the branch with the given name. This only means to delete the pointer associated with the branch; 
it does not mean to delete all commits that were created under the branch


### reset

```bash
java gitlet.Main reset [commit id]
```
Checks out all the files tracked by the given commit. Removes tracked files that are not present in that commit. 
Also moves the current branch’s head to that commit node.

This command is similar to using the --hard option in real git, 
as in `git reset --hard [commit hash]`.

### status
```bash
java gitlet.Main status
```
Displays what branches currently exist, and marks the current branch with a *.
Also displays what files have been staged for addition or removal.
The staging area is cleared. The command is essentially checkout of an arbitrary commit 
hat also changes the current branch head.

### log

```bash
java gitlet.Main log
```
Starting at the current head commit, display information about each commit backwards along the commit tree until the
initial commit, following the first parent commit links, ignoring any second parents found in merge commits.
(In regular Git, this is what you get with git log --first-parent). For every node in this history, the information
displayed is the commit id, the time the commit was made, and the commit message.


### global-log
```bash
java gitlet.Main global-log
```
Like log, except displays information about all commits ever made.
The commits are listed in the descending order of time, so the most recent commit will be printed at first place.


### find 
```bash
java gitlet.Main find [commit message]
```
Prints out the ids of all commits that have the given commit message, one per line. 
If there are multiple such commits, it prints the ids out on separate lines. 
The commit message is a single operand; to indicate a multiword message, put the operand in quotation marks.




<!--- Maybe I should mention the beautiful graph in the spec, and explain the data structure --->
