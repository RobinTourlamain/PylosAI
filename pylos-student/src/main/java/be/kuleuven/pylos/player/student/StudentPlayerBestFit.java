package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.*;

import static be.kuleuven.pylos.player.student.StudentPlayerBestFit.simulator;

public class StudentPlayerBestFit extends PylosPlayer{
    public static PylosGameSimulator simulator;
    public boolean isfirstmove = true;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        init(game.getState(), board);
        Action next = null;

        //eerste move ergens in midden
        if(isfirstmove){ //board.getNumberOfSpheresOnBoard() <= 3 && simulator.getState() == PylosGameState.MOVE
            for(int x = 1; x<=2; x++){
                for(int y = 1; y<=2; y++){
                    if(board.getBoardLocation(x, y,0).isUsable()){
                        next = new Action(Type.ADD, board.getReserve(this), board, null, board.getBoardLocation(x, y,0), game, PylosGameState.MOVE, this.PLAYER_COLOR);
                    }
                }
            }
            isfirstmove = false;
        }
        else{
            next = monteCarlo(game, board);
        }
        next.execute();
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        init(game.getState(), board);

        Action next = monteCarlo(game, board);
        next.execute();
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        init(game.getState(), board);

        Action next = monteCarlo(game, board);
        next.execute();
    }

    public void init(PylosGameState state, PylosBoard board){
        simulator  = new PylosGameSimulator(state, this.PLAYER_COLOR, board);
    }

    public Action monteCarlo(PylosGameIF game, PylosBoard board){
        Node root = new Node();
        List<Node> children = new ArrayList<>();
        for(Action action : generateAllActions(game, board, this)){
            Node newnode = new Node(new State(), root, action);
            children.add(newnode);
        }
        root.children = children;

        int iterations = 0;
        int maxtime = 50;
        long endtime = maxtime + System.currentTimeMillis();

        while(System.currentTimeMillis() < endtime){
            Node promising = selectPromising(root);

            if(simulator.getState() != PylosGameState.COMPLETED){
                expandNode(promising, game, board);
            }

            Node toexplore = promising;
            if(!promising.children.isEmpty()){
                toexplore = promising.getRandomChild();
                toexplore.transition.simulate();
            }


            PylosPlayerColor endresult = randomPlay(game, board, 0);
            //PylosPlayerColor endresult = evaluate(game, board);
            backPropagation(toexplore, endresult);
            iterations++;
        }

        System.out.println("#iterations: " + iterations);
        Node winner = root.getBestChild();
        return winner.transition;
    }

    public Node selectPromising(Node root){
        Node node = root;
        while(!node.children.isEmpty()){
            node = findBestNode(node);
            node.transition.simulate();
        }
        return node;
    }

    public Node findBestNode(Node node){
        int parentvisits = node.state.visitcount;
        return Collections.max(
          node.children,
          Comparator.comparing(
                  c -> uctValue(parentvisits, c.state.winrate, c.state.visitcount)
          )
        );
    }

    public double uctValue(int parentvisits, double winrate, int nodevisits){
        if(nodevisits == 0) return Integer.MAX_VALUE;
        return ((double) winrate / (double) nodevisits) + 1.41 * Math.sqrt(Math.log(parentvisits) / (double) nodevisits);
    }

    public void expandNode(Node node, PylosGameIF game, PylosBoard board){
        List<Action> actions;
        if(simulator.getColor() == this.PLAYER_COLOR){
            actions = generateAllActions(game, board, this);
        }else{
            actions = generateAllActions(game, board, this.OTHER);
        }
        List<Node> children = new ArrayList<>();
        for(Action action : actions){
            Node newnode = new Node(new State(), node, action);
            children.add(newnode);
        }
        node.children = children;
    }

    public PylosPlayerColor randomPlay(PylosGameIF game, PylosBoard board, int depth){
//        //early predicton
//        if(board.getReservesSize(this) - board.getReservesSize(this.OTHER) >= 5){
//            return this.PLAYER_COLOR;
//        }
//        //early stopping
//        if(depth > 40){
//            return evaluate(game, board);
//        }
//        depth++;

        PylosPlayer player;
        if(simulator.getColor() == this.PLAYER_COLOR){
            player = this;
        }
        else{
            player = this.OTHER;
        }

        if(simulator.getState() != PylosGameState.COMPLETED && simulator.getState() != PylosGameState.DRAW) {
            List<Action> actions = generateAllActions(game, board, player);
            Random rand = new Random();
            Action next = actions.get(rand.nextInt(actions.size()));
            next.simulate();
            PylosPlayerColor result =  randomPlay(game, board, depth);
            next.undo();
            return result;
        }else if(simulator.getState() == PylosGameState.COMPLETED){
            return simulator.getWinner();
        }else{
            return this.OTHER.PLAYER_COLOR;
        }
    }

    public PylosPlayerColor evaluate(PylosGameIF game, PylosBoard board){
        int prmRemove = 2;
        int prmDrie = 10;
        int score = 10 * (board.getReservesSize(this) - board.getReservesSize(this.OTHER));
        // berekening score voor aantal vierkanten met 3 ballen van hetzalfde kleur
        PylosSquare[] squares = board.getAllSquares();
        int i = 1;
        for (PylosSquare square : squares) {
            i++;
            // aantal ballen in het centrum belonen
            if (i==10)score+=(square.getInSquare(this)-square.getInSquare(this.OTHER))*prmRemove;
            // score voor 3 ballen bij elkaar met open plaats
            if(square.getInSquare(this)==3 && square.getInSquare(this.OTHER)!=1) score += prmDrie*2;
            else if(square.getInSquare(this.OTHER)==3 && square.getInSquare(this)!=1) score -= prmDrie*2;
                // score voor 3 ballen van de ander met 1 van ons die hem blok legt
            else if(square.getInSquare(this.OTHER)==3 && square.getInSquare(this)==1) score += prmDrie;
            else if(square.getInSquare(this)==3 && square.getInSquare(this.OTHER)==1) score -= prmDrie;
            // straf voor tegenstander die vierkant zou kunnen vormen
            //else if(square.getInSquare(this.OTHER)==3 && square.getInSquare(this)!=1) score -= prmDrie*2;
        }
        // berekening score voor aantal ballen die van het bord kunnen gehaald worden
        PylosSphere[] spheresPlayer = board.getSpheres(this);
        PylosSphere[] spheresOther = board.getSpheres(this.OTHER);
        for (PylosSphere sp : spheresPlayer){
            if (!sp.isReserve()){
                if (sp.canRemove()) score += prmRemove;
                score+=sp.getLocation().Z;
            }
        }
        for (PylosSphere sp : spheresOther){
            if (!sp.isReserve()){
                if (sp.canRemove()) score -= prmRemove;
                score-=sp.getLocation().Z;
            }
        }

        if(score > 0) return this.PLAYER_COLOR;
        return this.OTHER.PLAYER_COLOR;
    }

    public void backPropagation(Node node, PylosPlayerColor result){
        Node current = node;
        while(current.transition != null){
            current.state.visitcount++;
            if(result == this.PLAYER_COLOR && current.transition.prevcolor == this.PLAYER_COLOR){
                current.state.winrate += 1; //10
            }else if(result == this.OTHER.PLAYER_COLOR && current.transition.prevcolor == this.OTHER.PLAYER_COLOR){
                current.state.winrate += 1;
            }
            current.transition.undo();
            current = current.parent;
        }
        current.state.visitcount++;
        if(result == this.PLAYER_COLOR) current.state.winrate += 1; //10
    }

    public List<Action> generateAllActions(PylosGameIF game, PylosBoard board, PylosPlayer player){
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
                            if(sphere.canMoveTo(location)){
                                actions.add(new Action(Type.UPGRADE, sphere, board, sphere.getLocation(), location, game, PylosGameState.MOVE, player.PLAYER_COLOR));
                            }
                        }
                    }
                }

                //alle acties met reserve spheres
                PylosSphere myReserveSphere = board.getReserve(player);
                for(PylosLocation location : locations){
                    if(myReserveSphere.canMoveTo(location)){
                        actions.add(new Action(Type.ADD, myReserveSphere, board, null, location, game, PylosGameState.MOVE, player.PLAYER_COLOR));
                    }
                }
                break;
            case REMOVE_FIRST:
                for(PylosSphere sphere: board.getSpheres(player)){
                    if(sphere.canRemove()){
                        actions.add(new Action(Type.REMOVE_FIRST, sphere, board, sphere.getLocation(), null, game, PylosGameState.REMOVE_FIRST, player.PLAYER_COLOR));
                    }
                }
                break;
            case REMOVE_SECOND:
                for(PylosSphere sphere: board.getSpheres(player)){
                    if(sphere.canRemove()){
                        actions.add(new Action(Type.REMOVE_SECOND, sphere, board, sphere.getLocation(), null, game, PylosGameState.REMOVE_SECOND, player.PLAYER_COLOR));
                    }
                }
                actions.add(new Action(Type.PASS, null, board, null, null, game, PylosGameState.REMOVE_SECOND, player.PLAYER_COLOR));
                break;
        }

        return actions;
    }
}


class Node{
    State state = new State();
    Node parent = null;
    List<Node> children = new ArrayList<>();
    Action transition;

    public Node(){}
    public Node(State state, Node parent, Action transition){
        this.state = state;
        this.parent = parent;
        this.transition = transition;
    }

    public Node getRandomChild(){
        Random rand = new Random();
        return children.get(rand.nextInt(children.size()));
    }

    public Node getBestChild(){
        return Collections.max(
                children,
                Comparator.comparing(c -> c.state.winrate)
        );
    }
}

class State{
    int visitcount = 0;
    double winrate = 0;
}

class Action{
    public Type type;
    public PylosSphere sphere;
    public PylosBoard board;
    public PylosLocation prevlocation;
    public PylosLocation nextlocation;
    public PylosGameIF game;
    public PylosGameState prevState;
    public PylosPlayerColor prevcolor;

    Action(Type type, PylosSphere sphere, PylosBoard board, PylosLocation prev, PylosLocation next, PylosGameIF game, PylosGameState prevstate, PylosPlayerColor prevcolor){
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

enum Type{
    ADD,
    UPGRADE,
    REMOVE_FIRST,
    REMOVE_SECOND,
    PASS,
}
