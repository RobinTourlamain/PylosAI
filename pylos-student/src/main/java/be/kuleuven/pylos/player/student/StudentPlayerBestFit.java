package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static be.kuleuven.pylos.player.student.StudentPlayerBestFit.simulator;

public class StudentPlayerBestFit extends PylosPlayer{
    public static PylosGameSimulator simulator;
    public int MAXDEPTH = 2;
    public int cool = 0;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        init(game.getState(), board);
        List<Action> actions = generateAllActions(game, board, this);
//        Random rand = new Random();
//        Action next = actions.get(rand.nextInt(actions.size()));
        int bestScore = -9999999;
        int alpha = -9999999;
        int beta = 9999999;
        int diepte = 2;
        int score = 0;
        Action next = null;
        for (Action action : actions){
            cool++;
            System.out.println(cool);
            action.simulate();
            // TODO: een methode toevoegen die ballen removet & dus een aantal nieuwe mogelijke acties simuleert

            if(simulator.getColor() != this.PLAYER_COLOR){
                score = miniPlayer(game, board, alpha, beta, diepte);
            }
            else{
                score = maxiPlayer(game, board, alpha, beta, diepte);
            }

            if (score>bestScore){
                next = action;
                bestScore = score;
            }
            action.undo();
        }
        assert next != null;
        next.execute();
    }

    public int maxiPlayer(PylosGameIF game, PylosBoard board, int alpha, int beta, int diepte){
        int maxi = berekenScore(board);
        if(simulator.getState() == PylosGameState.COMPLETED){
            return maxi;
        }
        if (0 < board.getReservesSize(this)){
            List<Action> actions = generateAllActions(game, board, this);
            if (diepte>1){
                diepte-=1;
                for (Action action :actions) {
                    action.simulate();

                    //na deze simulate niet meer aan maxiplayer
                    if(simulator.getColor() != this.PLAYER_COLOR){
                        int score = miniPlayer(game, board, alpha, beta, diepte);
                        if (score >= beta) return score;
                        maxi = Math.max(maxi, score);
                    }
                    //na deze simulate nog eens maxiplayer
                    else{
                        maxi = maxiPlayer(game, board, alpha, beta, diepte);
                    }

                    alpha = Math.max(alpha, maxi);
                    action.undo();
                }
            }
        }

        return maxi;
    }
    public int miniPlayer(PylosGameIF game, PylosBoard board, int alpha, int beta, int diepte){
        int mini = berekenScore(board);
        if(simulator.getState() == PylosGameState.COMPLETED){
            return mini;
        }
        if (0 < board.getReservesSize(this.OTHER)){
            List<Action> actions = generateAllActions(game, board, this.OTHER);
            if (diepte>1){
                diepte-=1;
                for (Action action :actions) {
                    action.simulate();

                    //na deze simulate niet meer aan miniplayer
                    if(simulator.getColor() == this.PLAYER_COLOR){
                        int score = maxiPlayer(game, board, alpha, beta, diepte);
                        if (score <= alpha) return score;
                        mini = Math.min(mini, score);
                    }
                    //na deze simulate nog eens aan miniplayer
                    else{
                        mini = miniPlayer(game, board, alpha, beta, diepte);
                    }
                    action.undo();
                    beta = Math.min(beta, mini);
                }
            }
        }

        return mini;
    }

    public void init(PylosGameState state, PylosBoard board){
        simulator  = new PylosGameSimulator(state, this.PLAYER_COLOR, board);
    }

    public List<Action> generateAllActions(PylosGameIF game, PylosBoard board, PylosPlayer player){
        PylosSphere myReserveSphere = board.getReserve(player);
        PylosSphere[] mySpheres = board.getSpheres(player);
        PylosLocation[] locations = board.getLocations();
        List<Action> actions = new ArrayList<>();

        //bepaal welke acties van toepassing zijn in de simulatie
        switch(simulator.getState()){
            case MOVE:
                //alle acties met upgraden
                for(PylosSphere sphere : mySpheres){
                    if(!sphere.isReserve() && sphere.canMove()){
                        for(PylosLocation location : locations){
                            if(sphere.canMoveTo(location) && location.isUsable() && !location.hasAbove()){
                                actions.add(new Action(Type.UPGRADE, sphere, board, sphere.getLocation(), location, game));
                            }
                        }
                    }
                }

                //alle acties met reserve spheres
                for(PylosLocation location : locations){
                    if(myReserveSphere.canMoveTo(location) && location.isUsable()){
                        actions.add(new Action(Type.ADD, myReserveSphere, board, null, location, game));
                    }
                }
                break;
            case REMOVE_FIRST:
                for(PylosSphere sphere: board.getSpheres(player)){
                    if(sphere.canRemove()){
                        actions.add(new Action(Type.REMOVE_FIRST, sphere, board, sphere.getLocation(), null, game));
                    }
                }
                break;
            case REMOVE_SECOND:
                for(PylosSphere sphere: board.getSpheres(player)){
                    if(sphere.canRemove()){
                        actions.add(new Action(Type.REMOVE_SECOND, sphere, board, sphere.getLocation(), null, game));
                    }
                }
                break;
        }

        return actions;
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        init(game.getState(), board);

        //RANDOM
        List<PylosSphere> removable = new ArrayList<>();
        for(PylosSphere sphere: board.getSpheres(this)){
            if(sphere.canRemove()){
                removable.add(sphere);
            }
        }
        game.removeSphere(removable.get(0));
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        init(game.getState(), board);

        //RANDOM
        List<PylosSphere> removable = new ArrayList<>();
        for(PylosSphere sphere: board.getSpheres(this)){
            if(sphere.canRemove()){
                removable.add(sphere);
            }
        }
        if (removable.size()>0){
            game.removeSphere(removable.get(0));
        }
        else game.pass();
    }

    public int berekenScore(PylosBoard board){
        // toekenning gewicht voor aantal reserves
        int prmReserve = 10;
        // toekenning gewicht voor aantal keer 3 ballen naast elkaar liggen
        int prmDrie = 5;

        // berekening score voor aantal reserves
        int score =prmReserve * (board.getReservesSize(this.PLAYER_COLOR) - board.getReservesSize(this.OTHER));
        // berekening score voor aantal vierkanten met 3 ballen van hetzalfde kleur
        PylosSquare[] squares = board.getAllSquares();
        for (PylosSquare square : squares) {
            if(square.getInSquare(this)==3) score += prmDrie;
            if(square.getInSquare(this.OTHER)==3) score -= prmDrie;
        }

        return score;
    }
}

class Action{
    public Type type;
    public PylosSphere sphere;
    public PylosBoard board;
    public PylosLocation prevlocation;
    public PylosLocation nextlocation;
    public PylosGameIF game;
    public PylosGameState prevState = simulator.getState();
    public PylosPlayerColor prevcolor = simulator.getColor();

    Action(Type type, PylosSphere sphere, PylosBoard board, PylosLocation prev, PylosLocation next, PylosGameIF game){
        this.type = type;
        this.sphere = sphere;
        this.board = board;
        this.prevlocation = prev;
        this.nextlocation = next;
        this.game = game;
    }

    void execute(){
        switch(type){
            case ADD:
            case UPGRADE:
                game.moveSphere(sphere,nextlocation);
                break;
            case REMOVE_FIRST:
            case REMOVE_SECOND:
                game.removeSphere(sphere);
                break;
            case PASS:
                game.pass();
                break;
        }
    }

    void simulate(){
        switch(type){
            case ADD:
            case UPGRADE:
                simulator.moveSphere(sphere,nextlocation);
                break;
            case REMOVE_FIRST:
            case REMOVE_SECOND:
                simulator.removeSphere(sphere);
                break;
            case PASS:
                simulator.pass();
                break;
        }
    }

    void undo(){
        switch(type){
            case ADD:
                simulator.undoAddSphere(sphere,prevState,prevcolor);
                break;
            case REMOVE_FIRST:
                simulator.undoRemoveFirstSphere(sphere,prevlocation,prevState,prevcolor);
                break;
            case REMOVE_SECOND:
                simulator.undoRemoveSecondSphere(sphere,prevlocation,prevState,prevcolor);
                break;
            case UPGRADE:
                simulator.undoMoveSphere(sphere, prevlocation,prevState,prevcolor);
                break;
            case PASS:
                simulator.undoPass(prevState,prevcolor);
        }
    }
}

enum Type{
    ADD,
    UPGRADE,
    REMOVE_FIRST,
    REMOVE_SECOND,
    PASS,
}
