CONTENTS:
This directory contains a software implementation of a partial version control system, Gitlet.

"The point of a version-control system is to help you when creating complicated (or even not-so-complicated) projects, or when collaborating with others on a project. You save versions of the project periodically. If at some later point in time you accidentally mess up your code, then you can restore your source to a previously committed version (without losing any of the changes you made since then). If your collaborators make changes embodied in a commit, you can incorporate (merge) these changes into your own version."
	
Makefile	A makefile that will compile your
			files and run tests.  You must turn in a Makefile,
			'make' must compile all your files, and 
			'make check' must perform all your tests.  
			Currently, this makefile is set up to do just 
			that with our skeleton files.  Be sure to keep 
			it up to date.

gitlet/			Directory containing the Gitlet package.

    Repository.java	 	    Represents a file repository (as in Git)

    Main.java               Driver class for Gitlet, the tiny stupid version-control system.

    Commit.java             Represents a file commit object and stores all necessary metadata.

    Blob.java	            Represents a blob object, aka some individual piece on content within a commit.

    DumbObj.java            A debugging class used to print useful information about deserialized objects.