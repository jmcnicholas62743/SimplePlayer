package Controller;

import Model.*;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.control.*;
import javafx.scene.media.Media;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.XMPDM;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class mainController{

    private MediaPlayer player;
    private boolean paused;
    private DatabaseConnection database;
    private UserData user;
    File cwd = new File("Songs/").getAbsoluteFile();
    private ArrayList<TrackData> trackList = new ArrayList<>();


    @FXML private Label volumeLabel;
    @FXML private Label nameDisplayLabel;
    @FXML private Slider progressBar;
    @FXML private Slider volumeSlider;
    @FXML private Label currentSongText;
    @FXML private Label currentTimeLabel;
    @FXML private Label lengthLabel;
    @FXML protected void progressDragDropped(ActionEvent event) {
        System.out.println(event.toString());
        progressBar.valueProperty().addListener((observable, oldValue, newValue) -> {
            player.seek(Duration.seconds(newValue.intValue()));
        });
    }

    public void intitData(UserData user,DatabaseConnection d) {
        this.user = user;
        nameDisplayLabel.setText("Music Library - " + this.user.getUsername());

        database = d;
        volumeSlider.setMin(0);
        volumeSlider.setMax(100);
        volumeSlider.setValue(100);
        volumeLabel.setText("VOLUME: 100%");
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            volumeLabel.setText("VOLUME: " + newValue.intValue() + "%");
            player.setVolume(newValue.doubleValue() / 100);
        });

        progressBar.setMin(0);
    }

    @FXML protected void nextButtonPressed(ActionEvent event){ System.out.println("skip pressed"); }
    @FXML protected void prevButtonPressed(ActionEvent event){ System.out.println("prev pressed"); }

    @FXML protected void pauseButtonPressed(ActionEvent event){
        if(paused){
            player.play();
            System.out.println("Playing");
            paused = false;
        }else{
            player.pause();
            System.out.println("Paused");
            paused = true;
        }
    }

    @FXML protected void queueButtonPressed(ActionEvent event){ }

    @FXML protected void addSongButtonPressed(ActionEvent event) {
        //Opens file explorer and gets PATH to music
        Stage stage = new Stage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        configureFileChooser(fileChooser);

        List<File> filesToAdd = fileChooser.showOpenMultipleDialog(stage);
        if(filesToAdd != null){
            for(File f: filesToAdd){
                try {
                    copy(f,cwd);
                    File copiedSong = FileUtils.getFile("Songs",f.getName());
                    addSongToDatabase(getMetadata(copiedSong));
                    System.out.println("Success!");
                }catch (Exception e){
                    System.out.println(e.getMessage());
                }
            }
            loadIntoPlayer(filesToAdd.get(0));
        }
    }


    @FXML protected void removeSongButtonPressed(ActionEvent event){ System.out.println("song remove pressed"); }
    @FXML protected void playNextButtonPressed(ActionEvent event){ System.out.println("song will play next"); }
    @FXML protected void addToPlaylistButtonPressed(ActionEvent event){ System.out.println("added to playlist"); }

    public void initialize(){
        /*
            todo.Populate table with user's library
         */
        //ArtistColumn.setCellValueFactory(new PropertyValueFactory<>("Hello there"));
    }

    private static void configureFileChooser(final FileChooser fileChooser) {
        fileChooser.setTitle("View Music");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Music Files", "*.*"),
                new FileChooser.ExtensionFilter("MP3", "*.mp3"),
                new FileChooser.ExtensionFilter("WAV", "*.wav")
        );
    }


    public static void copy(File source, File cwd) throws IOException {
        FileUtils.copyFileToDirectory(source,cwd);
    }

    public void loadIntoPlayer(File f){
        /**
         * This should be moved to a play method
         */
        currentSongText.setText("Now playing - " + f.getName());
        try {
            Media pick = new Media(f.toURI().toURL().toString());
            player = new MediaPlayer(pick);

            player.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                if(!paused) {
                    progressBar.setValue(newValue.toSeconds());
                }
                currentTimeLabel.setText(formatTime((int)Math.round(newValue.toSeconds())));
            });

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        player.setOnReady(() -> {
            progressBar.setMax(player.getTotalDuration().toSeconds());
            lengthLabel.setText(formatTime((int)(Math.round(player.getTotalDuration().toSeconds()))));
            paused=true;
            //todo.implement scrubbing
        });

    }


    public TrackData getMetadata(File file) throws Exception{
        String fileLocation = file.getAbsolutePath();
        try {

            InputStream input = new FileInputStream(new File(fileLocation));
            ContentHandler handler = new DefaultHandler();
            Metadata metadata = new Metadata();
            Parser parser = new Mp3Parser();
            ParseContext parseCtx = new ParseContext();
            parser.parse(input, handler, metadata, parseCtx);
            input.close();


            //todo.implement method for fixing length & trackName

            int artistID = getArtistID(metadata.get("xmpDM:artist"));
            int duration = convertToSeconds(Double.parseDouble(metadata.get(XMPDM.DURATION)));
            return new TrackData((metadata.get("xmpDM:title")),duration,artistID,("Songs/"+file.getName()));

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
            } catch (IOException e) {
            System.out.println(e.getMessage());
            } catch (SAXException e) {
            System.out.println(e.getMessage());
            } catch (TikaException e) {
            System.out.println(e.getMessage());
            }
        return null;
    }

    public void addSongToDatabase(TrackData track){
        TrackDataService.selectAll(trackList, database);
        boolean trackExists = false;
        for (TrackData t : trackList) {
            if ((t.getPath().equals(track.getPath()))) {
                trackExists=true;
                break;
            }
        }
        if(!trackExists) {
            TrackDataService.save(track,database);
            System.out.println("Track successfully added!");
        }else{
            System.out.println("Track is already in library");
        }
    }

    public int getArtistID(String artist){
        ArrayList<ArtistData> artistList = new ArrayList<>();
        ArtistDataService.selectAll(artistList,database);
        for(ArtistData a:artistList){
            if(artist.equals(a.getArtistName())){
                return a.getArtistID();
            }
        }
        System.out.println("Artist not found");
        return -1;
        //todo.Add the artist here if they don't exist!
    }

    public int convertToSeconds(double milli){
        return (int)(milli/1000);
    }

    public String formatTime(int time){
        String formattedTime;
        if((time%60)<10){ formattedTime=((time /60)+":0"+(time%60));

        }else{ formattedTime=((time /60)+":"+(time%60)); }

        return formattedTime;
    }
}





