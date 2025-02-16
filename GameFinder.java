import com.mongodb.client.FindIterable;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MapReduceIterable;
import com.mongodb.client.model.Updates;
import org.bson.Document;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class GameFinder {
    private static final String DATABASE_NAME = "chess";
    private static final String GAMES_COLLECTION_NAME = "games";
    private static final Pattern SQUARE_BRACKET_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");
    private static final String INVERTED_FILE_COLLECTION_NAME = "inverted_file";

    private static Document parsePGNGame(String gameData) {
        Document gameDocument = new Document();
        Matcher matcher = SQUARE_BRACKET_PATTERN.matcher(gameData);

        while (matcher.find()) {
            String[] keyValue = matcher.group(1).split(" ", 2);
            gameDocument.append(keyValue[0], keyValue[1]);
        }

        String moves = gameData.substring(gameData.lastIndexOf("]") + 1).trim();
        moves = moves.replaceAll("\\n", "");
        gameDocument.append("Moves", moves);
        return gameDocument;
    }

    private static void ingest(String folder_path) {
        MongoClient client = new MongoClient();
        MongoDatabase db = client.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = db.getCollection(GAMES_COLLECTION_NAME);

        File folder = new File(folder_path);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".pgn"));

        if (files != null) {
            for(File file: files) {
                try{
                    List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                    StringBuilder gameBuilder = new StringBuilder();
                    String key = file.getName() + ":";
                    int lineNo = 1;

                    for(int i = 0; i<lines.size(); i++) {
                        String line = lines.get(i);
                        if (line.startsWith("[Event ")) {
                            if (gameBuilder.length() > 0) {
                                String gameData = gameBuilder.toString();
                                Document gameDocument = parsePGNGame(gameData);
                                collection.insertOne(new Document("_id", key + lineNo).append("game", gameDocument));
                                gameBuilder.setLength(0);
                            }
                            lineNo = i + 1;
                        }
                        gameBuilder.append(line).append("\n");
                    }

                    if(gameBuilder.length() > 0) {
                        String gameData = gameBuilder.toString();
                        Document gameDocument = parsePGNGame(gameData);
                        collection.insertOne(new Document("_id", key + lineNo).append("game", gameDocument));
                        gameBuilder.setLength(0);
                    }

                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

        client.close();
    }

    private static void mapReduce() {
        MongoClient mongoClient = new MongoClient();
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> gamesCollection = database.getCollection(GAMES_COLLECTION_NAME);
        String mapFunction = "function() {" + 
            "var moves = this.game.Moves.split(/\\s*\\d+\\.\\s*/).filter(Boolean);" +
            "moves = moves.map(item => item.split(' ')).flat();" +
            "for(var key in this.game){" + 
                "var term = encodeURIComponent(key + \":\" + this.game[key]);" + 
                "emit(term, this._id);" + 
            "}" +
            "for (var i = 0; i <= moves.length - 6; i++) {" + 
                "var term = moves.slice(i, i + 6).join(':').replace(/\\d+\\.\\s*/g, '');" + 
                "emit(term, this._id);" + 
            "}" + 
        "};";
        String reduceFunction = "function(key, values) {" + 
            "return Array.from(new Set(values)).sort();" + 
            "};";
        gamesCollection.mapReduce(mapFunction, reduceFunction)
                .collectionName(INVERTED_FILE_COLLECTION_NAME)
                .toCollection();
        mongoClient.close();

    }

    private static void appendIndent(StringBuilder stringBuilder, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            stringBuilder.append("  ");
        }
    }

    public static String prettyPrintJson(String jsonString) {

        jsonString = jsonString.replace("\"\\\"", "\"");
        jsonString = jsonString.replace("\\\"\"", "\"");
        jsonString = jsonString.replace("\\\\", "\\");
        StringBuilder prettyJson = new StringBuilder();
        int indentLevel = 0;
        boolean inQuotes = false;

        for (int i = 0; i < jsonString.length(); i++) {
            char character = jsonString.charAt(i);

            switch (character) {
                case '{':
                case '[':
                    prettyJson.append(character);
                    prettyJson.append("\n");
                    indentLevel++;
                    appendIndent(prettyJson, indentLevel);
                    break;
                case '}':
                case ']':
                    prettyJson.append("\n");
                    indentLevel--;
                    appendIndent(prettyJson, indentLevel);
                    prettyJson.append(character);
                    break;
                case ',':
                    prettyJson.append(character);
                    if (!inQuotes) {
                        prettyJson.append("\n");
                        appendIndent(prettyJson, indentLevel);
                    }
                    break;
                case '"':
                    prettyJson.append(character);
                    inQuotes = !inQuotes;
                    break;
                default:
                    prettyJson.append(character);
            }
        }

        return prettyJson.toString();
    }

    private static Pattern createSearchPattern(String term) {

        StringBuilder patternBuilder = new StringBuilder();
        patternBuilder.append(".*" + term + ".*");
        return Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
    }

    private static void query(String query) {
        MongoClient mongoClient = new MongoClient();
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> invertedFileCollection = database.getCollection(INVERTED_FILE_COLLECTION_NAME);
        String[] terms = query.split("\\s+");
        List<List<String>> game_ids = new ArrayList<>();
        for(int i = 0; i<terms.length; i++){
            FindIterable<Document> results = invertedFileCollection.find(new Document("_id", createSearchPattern(terms[i])));
            List<String> ids = new ArrayList<>();
            for(Document result: results){
                List<String> tmp_ids = (List<String>) result.get("value");
                ids.addAll(tmp_ids);
            }

            Set<String> set_ids = new LinkedHashSet<String>(ids);
            List<String> unique_ids = new ArrayList<String>(set_ids);
            game_ids.add(unique_ids);
        }

        if(game_ids.size() == 0) {
            return;
        }

        Set<String> common_ids = new LinkedHashSet<>(game_ids.get(0));
        for(int i = 1; i<game_ids.size(); i++) {
            Set<String> currList = new LinkedHashSet<>(game_ids.get(i));
            common_ids.retainAll(currList);
        }

        List<String> unique_game_ids = new ArrayList<String>(common_ids);

        MongoCollection<Document> gamesCollection = database.getCollection(GAMES_COLLECTION_NAME);
        System.out.println("The search results are as follows: \n");
        for(int i = 0; i<unique_game_ids.size(); i++) {
            Document game = gamesCollection.find(new Document("_id", unique_game_ids.get(i))).first();
            System.out.println(prettyPrintJson(game.toJson()));
        }

        mongoClient.close();
    }

    public static void main(String[] args) {
        if(args.length < 2){
            System.out.println("Expected more number of arguments.");
            System.out.println("Expected Usage: java GameFinder ingest some_folder");
            System.out.println("                java GameFinder query \\\"some_query\\\"");
        }

        if(args[0].equals("ingest")) {
            ingest(args[1]);
            mapReduce();
        }
        else if(args[0].equals("query")) {
            query(args[1]);
        }
        else{
            System.out.println("Unrecognized command. Did you mean 'ingest' or 'query'?");
        }
    }
}