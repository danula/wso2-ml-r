wso2-ml-r
=========

R extesion for Machine Learner


Instruction for setting up R and rJava on Ubuntu
================================================

1. Add the following line to /etc/apt/source.list where ${version} should be trusty or precise or utopic or lucid according to your Ubuntu release 
deb http://cran.rstudio.com/bin/linux/ubuntu ${version}/

2. Authenticate 
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys E084DAB9

3. Update 
sudo apt-get update

4. Install R 
sudo apt-get install r-base-dev r-cran-rjava