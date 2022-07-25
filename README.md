# Readme

"Gitlet" is a toy version of git designed by Joseph Moghadam from UC Berkeley. It contains some mostly used in git
such as init, add, commit, branch, checkout, etc.. This is also a project of UCB course CS61B: data structure. 

In this repository, I implemented my own version of "Gitlet" in Java, with the help of the skeleton codes & testing codes provided by UCB. 
For further details, please contact me through e-mail: eddiezhang@pku.edu.cn


## How to start Gitlet

First, clone this repository, and move to the root of your local repository in your shell (Gitlet is used through command line).
As you can see, there is a Makefile under the current path . 

Input command 'make', the java files 
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

### Init


```bash
java gitlet.Main init
```

Creates a new Gitlet version-control system in the current directory. 
This system will automatically start with one commit: a commit that contains no files 
and has the commit message "initial commit". 
It will have a single branch: master, which initially points to this initial commit, 
and master will be the current branch. The timestamp for this initial commit will be 00:00:00 UTC,
Thursday, 1 January 1970 in whatever format you choose for dates. 

## Ideas of Design

### The design of .gitlet directory

Maybe I should mention the beautiful graph in the spec, and explain the data structure