package com.theosirian.pacman;

import com.badlogic.gdx.math.Vector2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;

/**
 * <p>
 * </p>
 * <p>Created at 2015-09-13 19-43.</p>
 */
public class ServerConnection {

    final Game game;
    final int localPort;
    final String serverHost;
    final int serverPort;
    private String[] parameters;


    public ServerConnection(Game game) {
        this.game = game;
        this.localPort = 9001;
        this.serverPort = 9002;
        this.serverHost = "0.0.0.0";
    }

    private String connect(String message) {
        String r = null;
        try {
            //Runtime runtime = Runtime.getRuntime();
            //Process process = runtime.exec("./localServer " + this.localPort + " " + this.serverHost + " " + this.serverPort);
            //Thread.sleep(1000);

            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("./client " + this.serverHost + " " + this.serverPort + " " + message);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String s = reader.readLine();
            System.out.println(s);
            return s;
            /*
            Socket socket = new Socket(this.serverHost, this.serverPort);
            PrintStream printStream = new PrintStream(socket.getOutputStream());
            printStream.println(message);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            r = reader.readLine();
            reader.close();
            printStream.close();
            socket.close();
            */
        } catch (Exception e) {
            e.printStackTrace();
            r = null;
        }
        return r;
    }

    void init() {
        String response = connect("0");
        String[] parameters = response.split(";");
        if (Integer.parseInt(parameters[0]) == 0) {
            game.score = (Integer.parseInt(parameters[1]));
            game.lifes = (Integer.parseInt(parameters[2]));
            game.setStart(Instant.now());
        }
    }

    void nextLevel() {
        int score = (this.game.getPacman() != null) ? this.game.getPacman().getScore() : this.game.score;
        String response = connect("1;" + score);
        this.parameters = response.split(";");
        int header = Integer.parseInt(parameters[0]);
        if (header == 1) {
            this.game.transitMap(Integer.parseInt(parameters[parameters.length - 1]));
        } else if (header == 5) {
            this.game.win(Integer.parseInt(parameters[parameters.length - 1]));
        }
    }

    void ghostNextPosition(Ghost ghost) {
        String response = connect("2;" + ghost.getId() + ";" + this.game.getPacman().getTargetX() / 16 + ";" + this.game.getPacman().getTargetY() / 16);
        String[] parameters = response.split(";");
        int header = Integer.parseInt(parameters[0]);
        if (header == 2) {
            int x = Integer.parseInt(parameters[1]);
            int y = Integer.parseInt(parameters[2]);
            Vector2 objective = new Vector2(x * 16, y * 16);
            ghost.setObjectivePosition(objective);
        }
    }

    void death() {
        String response = connect("3;" + this.game.getPacman().getScore());
        String[] parameters = response.split(";");
        int header = Integer.parseInt(parameters[0]);
        if (header == 3) {
            this.game.getPacman().setLifeCounter(Integer.parseInt(parameters[1]));
        } else if (header == 6) {
            this.game.gameover(Integer.parseInt(parameters[1]));
        }
    }

    void bonusScore() {
        String response = connect("4;" + this.game.getPacman().getScore());
        String[] parameters = response.split(";");
        int header = Integer.parseInt(parameters[0]);
        if (header == 4) {
            this.game.getPacman().setScore(Integer.parseInt(parameters[1]));
        }
    }

    void eatGhost(Ghost ghost) {
        String response = connect("6;" + ghost.getId() + ";" + this.game.getPacman().getScore());
        String[] parameters = response.split(";");
        int header = Integer.parseInt(parameters[0]);
        if (header == 7) {
            int x = Integer.parseInt(parameters[1]);
            int y = Integer.parseInt(parameters[2]);
            int score = Integer.parseInt(parameters[3]);
            this.game.getPacman().setScore(score);
            Vector2 objective = new Vector2(x * 16, y * 16);
            ghost.setObjectivePosition(objective);
        }
        System.out.println(response);
    }

    int eatSuperDot(){
        String response = connect("5;" + this.game.getPacman().getScore());
        String[] parameters = response.split(";");
        int header = Integer.parseInt(parameters[0]);
        if (header == 8) {
            int score = Integer.parseInt(parameters[1]);
            int time = Integer.parseInt(parameters[2]);
            this.game.getPacman().setScore(score);
            return time;
        }
        return 0;
    }

    public String[] getParameters() {
        return parameters;
    }


}
