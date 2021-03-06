Disco - Collaborative Discourse Manager

This is an open-source project (see LICENSE file).
=====

[For information, bug reports, or to just let me know you are using
this code :-), please contact me at email address below - C. Rich]

This is an implementation of a collaborative discourse manager
inspired by the same collaborative discourse theory as Collagen (see
docs).  Disco uses the ANSI/CEA-2018 Task Model Description (CETask
1.0) standard for its task representation and is built on top of the
CETask reference implementation (see 'task' project below).

In addition, Disco also supports a more limited, but simpler,
dialogue-tree language (called D4g---Disco for Games) described in
docs/RichSidner2012_IVA.pdf

Disco is intended to be integrated with other systems, such as
appliances, games, robots, etc.  However, for demonstration and
development purposes, it also provides a simple command line
interface, which is an extension of the shell in the CETask
implementation.

   task
   disco
   examples
      callcenter
      DiscoUnity
      secrets
      tardis
      team
   release
     
The two main toplevel folders, task and disco, are Eclipse Java
projects containing all of the source code for Disco.  The task
project is the reference implementation for CETask; the disco project
depends on it and adds the focus stack and utterances.  The folders
under examples contain Eclipse Java projects for some examples built
using Disco.  You do not need to install these if you don't want to.

The release folder contains a precompiled version of Disco plus other
useful tools (see release/bin) for developing systems using Disco.

**NB** You do NOT need to build Disco to use it!

To simply run the precompiled version of the system, add
release/lib/disco.jar to your classpath and invoke main class
edu.wpi.disco.Disco.  You can also run the jar file directly as:

     java -jar release/lib/disco.jar

NB: This is pure Java software and is supported only in Java 8 (JRE
1.8) which includes the Nashorn JavaScript (ECMAScript) engine. Due to
a bug in early versions of Nashorn, this software requires Java
Version 8u60 or higher. It has been tested with Java SE 8u60 in the
Windows x64, Mac OS X x64 and Linux x64 releases from Oracle
(java.oracle.com). OpenJDK Java 8 releases are not available at time
of this writing.

The release-java7 folder and the v1.10 tag (Java 7 Archived Release)
in GitHub contain an archived copy of the last release of Disco built
in Java 7 (using the builtin Rhino JavaScript engine).

Note that disco.jar includes all the binary class files in both the
disco and task projects (but not the libraries in disco/d4g or
task/speech).

If you are using an IDE such as Eclipse or Netbeans, I strongly
recommend adding release/lib/disco-src.zip and disco-api.zip as the
source and Javadoc attachments for disco.jar.  This will greatly aid
development and debugging.

[Hint: to change the temporary directory where the log file is written
 to someplace more convenient, add a JRE argument, such as
 -Djava.io.tmpdir=/tmp ]

*** The best way to familiarize yourself further with Disco
    is to look next at disco/test/README.txt ***    

Please send bug reports to bug-disco@wpi.edu and make sure to
include both the Console.test file (from directory indicated at
system startup) and all task model files involved.

For Eclipse Users:

If you are developing your own system in Eclipse using Disco it would
be best to make your system be a *separate* project, rather than
adding your code to the Disco projects. Simply add the precompiled
disco.jar as a library of your new project (and don't forget to add
disco-src.zip and disco-api.zip attachments as discussed above).

However, if you do want to build Disco yourself, you can easily create
the two Eclipse projects (called task and disco) containing the source
by importing from the cloned repository:

          File > Import > Git > Projects from Git > Next 
          > Existing Local Repository > Disco > Next
	  > Select task and disco (or Select All to build all examples)
	  > Next > Finish
          
Then you can add the disco project to the project dependencies (see
Build Path) of your new project.  Note that the exports of the disco
project do not include the libraries for speech (see task/speech).

The current release was built in Eclipse 4.5 (Mars).

---

Dr. Charles Rich, Professor, Computer Science Department
Interactive Media and Game Development Program
Robotics Engineering Program
Worcester Polytechnic Institute, Fuller Laboratories B25b
100 Institute Road, Worcester, MA 01609-2280

Email: rich@wpi.edu   Phone: 508-831-5945   Fax: 508-831-5776
Home: http://www.cs.wpi.edu/~rich
