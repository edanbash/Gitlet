# Gitlet Design Document
Name: Michael Wildfeuer, Edan Bash


----------

Classes and Data Structures:

## Commit

This class represents the saved contents of an entire directory of files

**Fields**

1. String logMessage - commit message 
2. String date - timestamp of commit 
3. HashMap<String, Blob> blobs - mapping of file names to blob objects
4. Commit parent - the previous commit object
5. Commit mergeParent - parent for merge commits for the given branch 
6. String hashcode - hashcode for commit object generated by SHA-1


## Blob

This class represents the contents of the saved files 

**Fields**

1. String content - content of file

## Repository

This class keeps track of staged files and the current commit 

**Fields**

1. HashMap<String, Blob> addStage - staging area for files key being name of the file, and value being Blob object
2. HashMap<String, Blob> rmStage - staging area for remove files key being name of the file, and value being Blob object
3. HashMap<String, Commit> branches - maps branch name to head commit of that branch, key: Branch name, value: most current Commit object
4. HashMap<String, Commit> commits - maps commit ids to commits
5. Commit head - current working commit 
6. String currBranch - current working branch



----------
## **Algorithms**

**Repository** **Class**

1. Commit - create new commit object, add commit object to branches with key value pair being current branch name and commit object 
2. Add - add file to addStage, if current version of file is same as current commit then do not stage and remove if it is present in addStage
3. Remove - remove file if it it is present in addStage, if file is tracked in current commit then add file to rmStage and remove from working directory (if not already removed)
4. Branch - create new key value pair in branches with key being branch name and value being head
5. Find [commit message] - search through all commits backwards, if the message in a given commit are the same as commit message, store that commit id in a list, once done searching return that list of ids
6. Reset  [commit id] - checkout all files in current commit and remove all tracked files that are not in commit, move head of branch to specified commit node (essentially a checkout of random commit node)
7. Checkout:
    a. [file] - update files in the working directory to express the file state in the head commit
    b. [branch] - all files in the given branch head are placed into the current working directory, change current breach to given branch, any files present in the current branch but not in the checked out branch are deleted, clears staging area
    c. [commit] [file] update file in the working directory to express the file state in the given commit
8. Merge:
    1. Splitpoint - find split point of merge, do this by get head commit of each branch, moving backwards in each branch we will add each commit object into a designated Arraylist until there are two commit objects in both Arraylists that are the same, that commit object is the split point. 
    2. New blob - if commit in given branch contains blob not present in current branch add it to current branch, but the blob wasn’t present at the splitpoint 
    3. Remove blob - if a blob at split point is present and unchanged in current branch, but not present in given branch remove that blob from current branch commit 
    4. Modified blob (in given branch) - if blob is present at splitpoint and modified in given branch but unmodified in current branch, update blob in merge commit to represent the blob in the given branch 
    5. Modification conflict - print out contents of each version of file
        1. If the file was present at split point: the file has different contents in each branch, or the contents of one are changed and the other file is deleted
        2. If the file was absent at the split point: the file has different contents in the given and current branches. In this case, replace the contents of the conflicted file with


----------
## **Persistence**
1. Write the staging HashMaps (addStage, rmStage) to disk, so we can keep track of files that are staged for addition and removal. 
2. Write the branches HashMap to disk, so we can keep track of the the previous commits with the parentRef field. 
3. Write the commits HashMaps to disk, so we can keep track of which commits have which IDs.
4. Write the head commit to disk, so we can keep track of current commit.
5. Write the currBranch to disk so we know which branch we are working on.

Use the writeObject method from the Utils class to serialize this data into bytes that we can write to a specially named file on disk.

Before executing any code, we need to search for the saved files in the working directory and load the objects that we saved in them. Use a file naming convention so that file names are the same as the name of the object (“branches”, etc.) so we can easily find the objects. We can use the readObject method from the Utils class to read the data of files as and deserialize the objects we previously wrote to these files.

