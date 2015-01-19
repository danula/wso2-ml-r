wso2-ml-r
=========

R extesion for Machine Learner


<h2>Instruction for setting up R and rJava on Ubuntu</h2>

1. Add the following line to /etc/apt/source.list where ${version} should be trusty or precise or utopic or lucid according to your Ubuntu release<br> 
`deb http://cran.rstudio.com/bin/linux/ubuntu ${version}/`

2. Authenticate <br> 
`sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys E084DAB9`

3. Update <br> 
`sudo apt-get update`

4. Install R <br> 
`sudo apt-get install r-base-dev r-cran-rjava`

5. Run R and install following libraries<br>
`install.packages('e1071')`<br>
`install.packages('rpart')`<br>
`install.packages('randomForest')`<br>
`install.packages(‘pmml’)`<br>

<h2>Instruction for setting up the project</h2>

1. Before installing the R extensions, open a terminal and set R_HOME and JRI_HOME to the root folder of R and JRI respectively.<br>

2. Run mvn clean install 
