package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;
import java.util.ArrayList;
import java.util.List;

import static be.kuleuven.pylos.player.student.StudentPlayerTest.simulator;

public class StudentPlayerTest extends PylosPlayer{
    public static PylosGameSimulator simulator;
    public static int MAX_DEPTH = 6;
    public static int PRM_DRIE,PRM_REMOVE;

    public StudentPlayerTest(Integer drie, Integer remove){
        PRM_DRIE = drie;
        PRM_REMOVE = remove;
    }

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        init(game.getState(), board);
        List<Actiont> actions = generateAllActions(game, board, this);

        int bestscore = -Integer.MAX_VALUE;
        int alpha = -Integer.MAX_VALUE;
        int beta = Integer.MAX_VALUE;
        int depth = 0;
        int score = 0;

        Actiont next = null;
        for (Actiont action : actions){

            action.simulate();
            //score = descend(game, board, depth);
            score = branch(game, board, depth, alpha, beta);

            action.undo();

            if (score>bestscore){
                next = action;
                bestscore = score;
            }
            alpha = Math.max(alpha, bestscore);

        }
        assert next != null;
        next.execute();
    }

    public int branch(PylosGameIF game, PylosBoard board, int depth, int alpha, int beta){
        int score;
        if(simulator.getState() == PylosGameState.COMPLETED) return berekenScore(board);

//        if (simulator.getState() == PylosGameState.COMPLETED) {
//            if(simulator.getWinner() == this.PLAYER_COLOR){
//                return berekenScore(board) + depth;
//            }else {
//                return berekenScore(board) - depth;
//            }
//        }

        if(simulator.getColor() == this.PLAYER_COLOR){
            score = maxplayer(game, board, depth, alpha, beta);
        }
        else{
            score = minplayer(game, board, depth, alpha, beta);
        }
        return score;
    }

    public int maxplayer(PylosGameIF game, PylosBoard board, int depth, int alpha, int beta){
        int bestscore = berekenScore(board);
        List<Actiont> actions = generateAllActions(game, board, this);
        if(depth <= MAX_DEPTH){
            depth++;
            for(Actiont action : actions){
                action.simulate();

                int score = branch(game, board, depth, alpha, beta);
                bestscore = Math.max(bestscore, score);
                alpha = Math.max(alpha, bestscore);

                action.undo();
                if(beta <= alpha) break;
            }
        }
        return bestscore;
    }

    public int minplayer(PylosGameIF game, PylosBoard board, int depth, int alpha, int beta){
        int bestscore = berekenScore(board);
        List<Actiont> actions = generateAllActions(game, board, this.OTHER);

        if(depth <= MAX_DEPTH){
            depth++;
            for(Actiont action : actions){
                action.simulate();

                int score = branch(game, board, depth, alpha, beta);
                bestscore = Math.min(bestscore, score);
                beta = Math.min(alpha, bestscore);

                action.undo();
                if(beta <= alpha) break;
            }
        }
        return bestscore;
    }

    public int descend(PylosGameIF game, PylosBoard board, int depth){
        int score = berekenScore(board);
        //check of game gewonnen is
        if(simulator.getState() == PylosGameState.COMPLETED){
            return score;
        }

        PylosPlayer player;
        if(simulator.getColor() == this.PLAYER_COLOR){
            player = this;
        }
        else{
            player = this.OTHER;
        }

        List<Actiont> actions = generateAllActions(game, board, player);
        if (depth <= MAX_DEPTH){
            depth++;
            for (Actiont action : actions) {
                action.simulate();

                int nextscore = descend(game, board, depth);
                score = Math.max(score, nextscore);

                action.undo();
            }
        }
        return score;
    }

    public void init(PylosGameState state, PylosBoard board){
        simulator  = new PylosGameSimulator(state, this.PLAYER_COLOR, board);
    }

    public List<Actiont> generateAllActions(PylosGameIF game, PylosBoard board, PylosPlayer player){
        PylosSphere[] mySpheres = board.getSpheres(player);
        PylosLocation[] locations = board.getLocations();
        List<Actiont> actions = new ArrayList<>();

        //bepaal welke acties van toepassing zijn in de simulatie
        switch(simulator.getState()){
            case MOVE:
                //alle acties met upgraden
                for(PylosSphere sphere : mySpheres){
                    if(!sphere.isReserve() && sphere.canMove()){
                        for(PylosLocation location : locations){
                            if(sphere.canMoveTo(location)){
                                actions.add(new Actiont(Type.UPGRADE, sphere, board, sphere.getLocation(), location, game, PylosGameState.MOVE, player.PLAYER_COLOR));
                            }
                        }
                    }
                }

                //alle acties met reserve spheres
                PylosSphere myReserveSphere = board.getReserve(player);
                for(PylosLocation location : locations){
                    if(myReserveSphere.canMoveTo(location)){
                        actions.add(new Actiont(Type.ADD, myReserveSphere, board, null, location, game, PylosGameState.MOVE, player.PLAYER_COLOR));
                    }
                }
                break;
            case REMOVE_FIRST:
                for(PylosSphere sphere: board.getSpheres(player)){
                    if(sphere.canRemove()){
                        actions.add(new Actiont(Type.REMOVE_FIRST, sphere, board, sphere.getLocation(), null, game, PylosGameState.REMOVE_FIRST, player.PLAYER_COLOR));
                    }
                }
                break;
            case REMOVE_SECOND:
                for(PylosSphere sphere: board.getSpheres(player)){
                    if(sphere.canRemove()){
                        actions.add(new Actiont(Type.REMOVE_SECOND, sphere, board, sphere.getLocation(), null, game, PylosGameState.REMOVE_SECOND, player.PLAYER_COLOR));
                    }
                }
                actions.add(new Actiont(Type.PASS, null, board, null, null, game, PylosGameState.REMOVE_SECOND, player.PLAYER_COLOR));
                break;
        }

        return actions;
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        init(game.getState(), board);
        List<Actiont> actions = generateAllActions(game, board, this);

        int bestScore = -Integer.MAX_VALUE;
        int alpha = -Integer.MAX_VALUE;
        int beta = Integer.MAX_VALUE;
        int depth = 0;
        int score = 0;

        Actiont next = null;
        for (Actiont action : actions){

            action.simulate();
            //score = descend(game, board, depth);
            score = branch(game, board, depth, alpha, beta);

            if (score>bestScore){
                next = action;
                bestScore = score;
            }
            action.undo();
        }
        assert next != null;
        next.execute();
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        init(game.getState(), board);
        List<Actiont> actions = generateAllActions(game, board, this);

        int bestScore = -Integer.MAX_VALUE;
        int alpha = -Integer.MAX_VALUE;
        int beta = Integer.MAX_VALUE;
        int depth = 0;
        int score = 0;

        Actiont next = null;
        for (Actiont action : actions){

            action.simulate();
            //score = descend(game, board, depth);
            score = branch(game, board, depth, alpha, beta);

            if (score>bestScore){
                next = action;
                bestScore = score;
            }
            action.undo();
        }
        assert next != null;
        next.execute();
    }

    public int berekenScore(PylosBoard board){
        // toekenning gewicht voor aantal reserves
        int prmReserve = 10;
        // toekenning gewicht voor aantal keer 3 ballen naast elkaar liggen
        int prmDrie = PRM_DRIE;
        // toekenning gewicht voor aantal verwijderbare ballen
        int prmRemove = PRM_REMOVE;

        // berekening score voor aantal reserves
        int score =prmReserve * (board.getReservesSize(this) - board.getReservesSize(this.OTHER));
        // berekening score voor aantal vierkanten met 3 ballen van hetzalfde kleur
        PylosSquare[] squares = board.getAllSquares();
        int i = 1;
        for (PylosSquare square : squares) {
            i++;
            // aantal ballen in het centrum belonen
            if (i==10)score+=(square.getInSquare(this)-square.getInSquare(this.OTHER))*prmDrie;
            // score voor 3 ballen bij elkaar met open plaats
            if(square.getInSquare(this)==3 && square.getInSquare(this.OTHER)!=1) score += prmDrie*2;
            // score voor 3 ballen van de ander met 1 van ons die hem blok legt
            else if(square.getInSquare(this.OTHER)==3 && square.getInSquare(this)==1) score += prmDrie;
            // straf voor tegenstander die vierkant zou kunnen vormen
            else if(square.getInSquare(this.OTHER)==3 && square.getInSquare(this)!=1) score -= prmDrie*2;
        }
        // berekening score voor aantal ballen die van het bord kunnen gehaald worden
        PylosSphere[] spheresPlayer = board.getSpheres(this);
        PylosSphere[] spheresOther = board.getSpheres(this.OTHER);
        for (PylosSphere sp : spheresPlayer){
            if (sp.canRemove()) score += prmRemove;
        }
        for (PylosSphere sp : spheresOther){
            if (!sp.canRemove()) score += prmRemove;
        }

        return score;
    }
}

class Actiont{
    public Type type;
    public PylosSphere sphere;
    public PylosBoard board;
    public PylosLocation prevlocation;
    public PylosLocation nextlocation;
    public PylosGameIF game;
    public PylosGameState prevState;
    public PylosPlayerColor prevcolor;

    Actiont(Type type, PylosSphere sphere, PylosBoard board, PylosLocation prev, PylosLocation next, PylosGameIF game, PylosGameState prevstate, PylosPlayerColor prevcolor){
        this.type = type;
        this.sphere = sphere;
        this.board = board;
        this.prevlocation = prev;
        this.nextlocation = next;
        this.game = game;
        this.prevState = prevstate;
        this.prevcolor = prevcolor;
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
                assert simulator.getState() == prevState && simulator.getColor() == prevcolor;
                break;
            case REMOVE_FIRST:
                simulator.undoRemoveFirstSphere(sphere,prevlocation,prevState,prevcolor);
                assert simulator.getState() == prevState && simulator.getColor() == prevcolor;
                break;
            case REMOVE_SECOND:
                simulator.undoRemoveSecondSphere(sphere,prevlocation,prevState,prevcolor);
                assert simulator.getState() == prevState && simulator.getColor() == prevcolor;
                break;
            case UPGRADE:
                simulator.undoMoveSphere(sphere, prevlocation,prevState,prevcolor);
                assert simulator.getState() == prevState && simulator.getColor() == prevcolor;
                break;
            case PASS:
                simulator.undoPass(prevState,prevcolor);
                assert simulator.getState() == prevState && simulator.getColor() == prevcolor;
                break;
        }
    }
}

enum Typet{
    ADD,
    UPGRADE,
    REMOVE_FIRST,
    REMOVE_SECOND,
    PASS,
}

