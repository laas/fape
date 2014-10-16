#!/bin/sh

# clone dependencies in read-only mode
# for read write access, you should replace git:// with ssh://
if [ ! -d "graph" ]; then
  git clone git://trac.laas.fr/git/robots/fape-graphs graph
fi

if [ ! -d "constraints" ]; then
  git clone git://trac.laas.fr/git/robots/fape-constraints constraints
fi

if [ ! -d "anml" ]; then
  git clone git://trac.laas.fr/git/robots/fape-anml anml
fi

# for each repository, pull for latest changes, build and publish 
# to local ivy store (should be accessible for building other projects)

cd graph
git pull
sbt publishLocal
cd ../constraints
git pull
sbt publishLocal
cd ../anml
git pull
sbt publishLocal
