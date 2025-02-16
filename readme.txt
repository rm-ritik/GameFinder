#This part of the assignment aims to implement GameFinder.java

#To complile the file the following command can be used:

javac -cp .:path/to/jar_file/for/mongodb/java/driver GameFinder.java 

#In order to compile the project using the mongodb java driver included in the project for reference, use the following command:

javac -cp .:mongo-java-driver-3.12.14.jar GameFinder.java

#GameFinder.java can be used to perform two operations:

#1. java -cp .:mongo-java-driver-3.12.14.jar GameFinder.java ingest some_folder
#2. java -cp .:mongo-java-driver-3.12.14.jar GameFinder.java query "some_query"

#The constants defined in GameFinder.java are as follows:
#DATABASE_NAME: name of the mongodb database that the application will connect to.
#GAMES_COLLECTION_NAME: collection name to store the games
#SQUARE_BRACKET_PATTERN: pattern to match the square bracket pattern such as [Site "Sousse"] in games
#INVERTED_FILE_COLLECTION_NAME: collection name of inverted_file
