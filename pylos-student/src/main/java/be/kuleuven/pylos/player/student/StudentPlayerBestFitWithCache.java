package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;
import java.util.*;

import static be.kuleuven.pylos.player.student.StudentPlayerBestFitWithCache.simulator;

//player die boom opslaat en hergebruikt
public class StudentPlayerBestFitWithCache extends PylosPlayer{
    public static PylosGameSimulator simulator;
    public Node root = null;
    public Map<Long, Node> nextstates;
    final long MAX_TIME_PER_MOVE = 50;
    public boolean isfirstmove = true;
    public double exparam = 1.41;

    public StudentPlayerBestFitWithCache(){}
    public StudentPlayerBestFitWithCache(double explorationparameter){exparam = explorationparameter;}

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        init(game.getState(), board);
        Action next = null;

        //eerste move ergens in midden
        if(isfirstmove){ //board.getNumberOfSpheresOnBoard() <= 3 && simulator.getState() == PylosGameState.MOVE
            for(int x = 1; x<=2; x++){
                for(int y = 1; y<=2; y++){
                    if(board.getBoardLocation(x, y,0).isUsable()){
                        next = new Action(Type.ADD, board.getReserve(this), null, board.getBoardLocation(x, y,0), game, PylosGameState.MOVE, this.PLAYER_COLOR);
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
        if(this.root == null || !canReuse(game, board)) {
            this.root = new Node();
            List<Node> children = new ArrayList<>();
            for (Action action : generateAllActions(game, board, this)) {
                Node newnode = new Node(new State(), root, action);
                children.add(newnode);
            }
            root.children = children;
        }

        int iterations = 0;
        long endtime = MAX_TIME_PER_MOVE + System.currentTimeMillis();

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

            if(endresult == null){
                backPropagationDraw(toexplore);
            }else if(endresult == this.PLAYER_COLOR){
                backPropagationWin(toexplore);
            }else{
                backPropagationLoss(toexplore);
            }

            //backPropagation(toexplore, endresult);
            iterations++;
        }

//        System.out.println("#iterations: " + iterations);
        Node winner = root.getBestChild();
        this.root = winner;

        winner.transition.simulate();
        saveNextPossibleStates(game, board);
        winner.transition.undo();
        return winner.transition;
    }

    public boolean canReuse(PylosGameIF game, PylosBoard board){
        if(this.nextstates.isEmpty()) return false;
        long currentstate = board.toLong();
        for(Map.Entry<Long, Node> entry: nextstates.entrySet()){
            if(currentstate == entry.getKey() && entry.getValue().parent == this.root){
                this.root.children = null;
                this.root = entry.getValue();
                this.root.transition = null;
                this.root.parent = null;
                return true;
            }
        }
        return false;
    }

    public void saveNextPossibleStates(PylosGameIF game, PylosBoard board){
        nextstates = new HashMap<>();
        for(Node node: this.root.children){
            node.transition.simulate();
            if(simulator.getColor() == this.PLAYER_COLOR){
                nextstates.put(board.toLong(), node);
            }
            node.transition.undo();
        }
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
//        //early prediction
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
            return null;
        }
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

    private void backPropagationLoss(Node node){
        Node current = node;
        while(current.transition != null){
            current.state.visitcount++;
            if(current.transition.prevcolor == this.OTHER.PLAYER_COLOR){
                current.state.winrate += 1;
            }
            current.transition.undo();
            current = current.parent;
        }
        current.state.visitcount++;
    }

    private void backPropagationWin(Node node){
        Node current = node;
        while(current.transition != null){
            current.state.visitcount++;
            if(current.transition.prevcolor == this.PLAYER_COLOR){
                current.state.winrate += 1;
            }
            current.transition.undo();
            current = current.parent;
        }
        current.state.visitcount++;
        current.state.winrate += 1;
    }

    private void backPropagationDraw(Node node){
        Node current = node;
        while(current.transition != null){
            current.state.visitcount++;
            current.transition.undo();
            current = current.parent;
        }
        current.state.visitcount++;
    }

    public List<Action> generateAllActions(PylosGameIF game, PylosBoard board, PylosPlayer player){
        PylosSphere[] mySpheres = board.getSpheres(player);
        PylosLocation[] locations = board.getLocations();
        List<Action> actions = new ArrayList<>();

        switch(simulator.getState()){
            case MOVE:
                //alle acties met upgraden
                for(PylosSphere sphere : mySpheres){
                    if(!sphere.isReserve() && sphere.canMove()){
                        for(PylosLocation location : locations){
                            if(sphere.canMoveTo(location)){
                                actions.add(new Action(Type.UPGRADE, sphere, sphere.getLocation(), location, game, PylosGameState.MOVE, player.PLAYER_COLOR));
                            }
                        }
                    }
                }

                //alle acties met reserve spheres
                PylosSphere myReserveSphere = board.getReserve(player);
                for(PylosLocation location : locations){
                    if(myReserveSphere.canMoveTo(location)){
                        actions.add(new Action(Type.ADD, myReserveSphere, null, location, game, PylosGameState.MOVE, player.PLAYER_COLOR));
                    }
                }
                break;
            case REMOVE_FIRST:
                for(PylosSphere sphere: board.getSpheres(player)){
                    if(sphere.canRemove()){
                        actions.add(new Action(Type.REMOVE_FIRST, sphere, sphere.getLocation(), null, game, PylosGameState.REMOVE_FIRST, player.PLAYER_COLOR));
                    }
                }
                break;
            case REMOVE_SECOND:
                for(PylosSphere sphere: board.getSpheres(player)){
                    if(sphere.canRemove()){
                        actions.add(new Action(Type.REMOVE_SECOND, sphere, sphere.getLocation(), null, game, PylosGameState.REMOVE_SECOND, player.PLAYER_COLOR));
                    }
                }
                actions.add(new Action(Type.PASS, null, null, null, game, PylosGameState.REMOVE_SECOND, player.PLAYER_COLOR));
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

class Action{
    public Type type;
    public PylosSphere sphere;
    public PylosLocation prevlocation;
    public PylosLocation nextlocation;
    public PylosGameIF game;
    public PylosGameState prevState;
    public PylosPlayerColor prevcolor;

    Action(Type type, PylosSphere sphere, PylosLocation prev, PylosLocation next, PylosGameIF game, PylosGameState prevstate, PylosPlayerColor prevcolor){
        this.type = type;
        this.sphere = sphere;
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
