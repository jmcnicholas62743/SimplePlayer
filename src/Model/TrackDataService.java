package Model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static Model.DatabaseConnection.deleteFromTable;

public class TrackDataService {

    public static void selectAll(List<TrackData> targetList, DatabaseConnection database){
        PreparedStatement statement = database.newStatement("SELECT trackID, trackName, length, artistID, path FROM TrackData ORDER BY trackID");
        try{
            if(statement != null){
                ResultSet results = database.executeQuery(statement);

                if(results!=null){
                    while(results.next()){
                        targetList.add(new TrackData(
                                results.getInt("trackID"),
                                results.getString("trackName"),
                                results.getInt("length"),
                                results.getInt("artistID"),
                                results.getString("path")
                        ));
                    }
                }
            }
        } catch(SQLException resultsException){
            System.out.println("Database select all error: "+resultsException.getMessage());
        }
    }


    public static TrackData selectByID(int id, DatabaseConnection database){
        TrackData result = null;
        PreparedStatement statement = database.newStatement("SELECT trackID, trackName, length, artistID, path FROM TrackData WHERE trackID = ?");

        try {
            if (statement != null) {

                statement.setInt(1, id);
                ResultSet results = database.executeQuery(statement);

                if (results != null) {
                    result = new TrackData(
                            results.getInt("trackID"),
                            results.getString("trackName"),
                            results.getInt("length"),
                            results.getInt("artistID"),
                            results.getString("path"));
                }
            }
        } catch (SQLException resultsException) {
            System.out.println("Database select by id error: " + resultsException.getMessage());
        }
        return result;
    }


    public static void save(TrackData itemToSave, DatabaseConnection database){
        TrackData existingItem = null;
        if (itemToSave.getTrackID() != 0){
            existingItem = selectByID(itemToSave.getTrackID(), database);
        }

        try {
            if (existingItem == null) {
                PreparedStatement statement = database.newStatement("INSERT INTO TrackData (trackName, length, artistID, path) VALUES (?, ?, ?, ?)");
                statement.setString(1, itemToSave.getTrackName());
                statement.setInt(2,itemToSave.getLength());
                statement.setInt(3,itemToSave.getArtistID());
                statement.setString(4,itemToSave.getPath());
                database.executeUpdate(statement);
            }
            else {
                PreparedStatement statement = database.newStatement("UPDATE TrackData SET trackName, length, artistID, path = ?, WHERE trackID = ?");
                statement.setString(1, itemToSave.getTrackName());
                statement.setInt(2,itemToSave.getLength());
                statement.setInt(3,itemToSave.getArtistID());
                statement.setString(4,itemToSave.getPath());
                database.executeUpdate(statement);
            }
        } catch (SQLException resultsException) {
            System.out.println("Database saving error: " + resultsException.getMessage());
        }
    }


    public static void deleteByID(int id, DatabaseConnection database){
        PreparedStatement statement = database.newStatement("DELETE FROM TrackData WHERE trackID = ?");
        deleteFromTable(id,database,statement);
    }

}
