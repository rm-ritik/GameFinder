# GameFinder
A MongoDB-Based Chess Game Search Engine

Project Overview:
The GameFinder project is designed to process and analyze large sets of Chess games in PGN format using MongoDB for storage and MapReduce for efficient term indexing. The program extracts and stores Chess games from PGN files into a MongoDB collection and then performs advanced queries to retrieve games based on specific terms or move sequences.

Features:
Ingest PGN Files:
Connects to a MongoDB database, ingests PGN files, and stores game data in a structured format. The games are indexed by unique keys that are based on the filename and line number of the game in the PGN file.
MapReduce for Term Indexing:
Uses MongoDB's MapReduce functionality to create an inverted index of terms. These terms are extracted from the metadata (e.g., player names, event names) and the move sequences of the games.
Search and Query:
Supports complex search queries where users can input terms, and the program returns all the games containing those terms in lexicographically sorted order.

Features:

Ingest and store Chess games in MongoDB from PGN files.
Create an inverted index using MongoDB’s MapReduce.
Run queries on the index to retrieve Chess games that contain specific search terms.
How it works:

Ingest command processes PGN files, extracts game data, and stores it in a MongoDB collection.
MapReduce is applied to generate an inverted index of terms, which can be queried for fast searches on game metadata and move sequences.
Query command retrieves games that match search terms, displaying the results in lexicographical order.

Commands:

java GameFinder ingest "some_folder"
java GameFinder query "some_query"

Example usage:
Ingest PGN files into MongoDB:
java GameFinder ingest /path/to/pgn/files

Query for games containing specific terms:
java GameFinder query "Source%3A%22ChessBase%22 e4:e5:Nf3"

The project demonstrates the power of MapReduce and MongoDB for efficiently processing and querying large datasets of structured and unstructured information like Chess games.
