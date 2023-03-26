package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.*;
import static be.kuleuven.pylos.player.student.StudentPlayerBestFitWithoutCache.simulatorcacheless;

//player zonder boom op te slaan
public class StudentPlayerBestFitWithoutCache extends PylosPlayer{
    public static PylosGameSimulator simulatorcacheless;
    public Random randomgenerator = new Random();
    final long MAX_TIME_PER_MOVE = 40;
    public boolean isfirstmove = true;
    public double exparam = 1.41;

    public StudentPlayerBestFitWithoutCache(){}
    public StudentPlayerBestFitWithoutCache(double explorationparameter){
        exparam = explorationparameter;
    }

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        init(game.getState(), board);
        Actioncacheless next = null;

        //eerste move ergens in midden
        if(isfirstmove){ //board.getNumberOfSpheresOnBoard() <= 3 && simulator.getState() == PylosGameState.MOVE
            for(int x = 1; x<=2; x++){
                for(int y = 1; y<=2; y++){
                    if(board.getBoardLocation(x, y,0).isUsable()){
                        next = new Actioncacheless(Type.ADD, board.getReserve(this), null, board.getBoardLocation(x, y,0), game, PylosGameState.MOVE, this.PLAYER_COLOR);
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

        Actioncacheless next = monteCarlo(game, board);
        next.execute();
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        init(game.getState(), board);

        Actioncacheless next = monteCarlo(game, board);
        next.execute();
    }

    public void init(PylosGameState state, PylosBoard board){
        simulatorcacheless  = new PylosGameSimulator(state, this.PLAYER_COLOR, board);
    }

    public Actioncacheless monteCarlo(PylosGameIF game, PylosBoard board){
        //root van boom instellen op huidige staat bord
        Nodecacheless root = new Nodecacheless();
        List<Nodecacheless> children = new ArrayList<>();
        for(Actioncacheless action : generateAllActions(game, board, this)){
            Nodecacheless newnode = new Nodecacheless(new State(), root, action);
            children.add(newnode);
        }
        root.children = children;

        long endtime = MAX_TIME_PER_MOVE + System.currentTimeMillis();
        while(System.currentTimeMillis() < endtime){
            //selecteer beste node in huidige tree
            Nodecacheless promising = selectPromising(root);

            if(simulatorcacheless.getState() != PylosGameState.COMPLETED){
                //genereer kinderen van de promising node
                expandNode(promising, game, board);
            }

            Nodecacheless toexplore = promising;
            if(!promising.children.isEmpty()){
                //selecteer random kind van promising node
                toexplore = promising.getRandomChild();
                toexplore.transition.simulate();
            }

            //speel vanuit de huidige node random tot een eindresultaat is bereikt
            PylosPlayerColor endresult = randomPlay(game, board);
            //propageer resultaat terug door alle nodes tot en met root
            if(endresult == null){
                backPropagationDraw(toexplore);
            }else if(endresult == this.PLAYER_COLOR){
                backPropagationWin(toexplore);
            }else{
                backPropagationLoss(toexplore);
            }
//            backPropagation(toexplore, endresult);
        }

        //geef beste kind van root terug als move om te maken
        Nodecacheless winner = root.getBestChild();
        return winner.transition;
    }

    private void backPropagationLoss(Nodecacheless node){
        Nodecacheless current = node;
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

    private void backPropagationWin(Nodecacheless node){
        Nodecacheless current = node;
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

    private void backPropagationDraw(Nodecacheless node){
        Nodecacheless current = node;
        while(current.transition != null){
            current.state.visitcount++;
            current.transition.undo();
            current = current.parent;
        }
        current.state.visitcount++;
    }

    public Nodecacheless selectPromising(Nodecacheless root){
        Nodecacheless node = root;
        while(!node.children.isEmpty()){
            node = findBestNode(node);
            node.transition.simulate();
        }
        return node;
    }

    public Nodecacheless findBestNode(Nodecacheless node){
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
        return ((double) winrate / (double) nodevisits) + exparam * Math.sqrt(Math.log(parentvisits) / (double) nodevisits);
    }

    public void expandNode(Nodecacheless node, PylosGameIF game, PylosBoard board){
        List<Actioncacheless> actions;
        if(simulatorcacheless.getColor() == this.PLAYER_COLOR){
            actions = generateAllActions(game, board, this);
        }else{
            actions = generateAllActions(game, board, this.OTHER);
        }
        List<Nodecacheless> children = new ArrayList<>();
        for(Actioncacheless action : actions){
            Nodecacheless newnode = new Nodecacheless(new State(), node, action);
            children.add(newnode);
        }
        node.children = children;
    }

    public PylosPlayerColor randomPlay(PylosGameIF game, PylosBoard board){
//        //early predicton
//        if(board.getReservesSize(this) - board.getReservesSize(this.OTHER) >= 5){
//            return this.PLAYER_COLOR;
//        }
//        //early stopping
//        if(depth > 40){
//            return evaluate(game, board);
//        }
        if(simulatorcacheless.getState() == PylosGameState.COMPLETED) return simulatorcacheless.getWinner();

        PylosPlayer player;
        if(simulatorcacheless.getColor() == this.PLAYER_COLOR){
            player = this;
        }
        else{
            player = this.OTHER;
        }

        if(simulatorcacheless.getState() != PylosGameState.DRAW) {
            List<Actioncacheless> actions = generateAllActions(game, board, player);
            Actioncacheless next = actions.get(randomgenerator.nextInt(actions.size()));
            next.simulate();
            PylosPlayerColor result =  randomPlay(game, board);
            next.undo();
            return result;
        }else{
            return null;
        }
    }

    public List<Actioncacheless> generateAllActions(PylosGameIF game, PylosBoard board, PylosPlayer player){
        PylosSphere[] mySpheres = board.getSpheres(player);
        PylosLocation[] locations = board.getLocations();
        List<Actioncacheless> actions = new ArrayList<>();

        //bepaal welke acties van toepassing zijn in de simulatie
        switch(simulatorcacheless.getState()){
            case MOVE:
                //alle acties met upgraden
                for(PylosSphere sphere : mySpheres){
                    if(!sphere.isReserve() && sphere.canMove()){
                        for(PylosLocation location : locations){
                            if(sphere.canMoveTo(location)){
                                actions.add(new Actioncacheless(Type.UPGRADE, sphere, sphere.getLocation(), location, game, PylosGameState.MOVE, player.PLAYER_COLOR));
                            }
                        }
                    }
                }

                //alle acties met reserve spheres
                PylosSphere myReserveSphere = board.getReserve(player);
                for(PylosLocation location : locations){
                    if(myReserveSphere.canMoveTo(location)){
                        actions.add(new Actioncacheless(Type.ADD, myReserveSphere, null, location, game, PylosGameState.MOVE, player.PLAYER_COLOR));
                    }
                }
                break;
            case REMOVE_FIRST:
                for(PylosSphere sphere: board.getSpheres(player)){
                    if(sphere.canRemove()){
                        actions.add(new Actioncacheless(Type.REMOVE_FIRST, sphere, sphere.getLocation(), null, game, PylosGameState.REMOVE_FIRST, player.PLAYER_COLOR));
                    }
                }
                break;
            case REMOVE_SECOND:
                for(PylosSphere sphere: board.getSpheres(player)){
                    if(sphere.canRemove()){
                        actions.add(new Actioncacheless(Type.REMOVE_SECOND, sphere, sphere.getLocation(), null, game, PylosGameState.REMOVE_SECOND, player.PLAYER_COLOR));
                    }
                }
                actions.add(new Actioncacheless(Type.PASS, null, null, null, game, PylosGameState.REMOVE_SECOND, player.PLAYER_COLOR));
                break;
        }

        return actions;
    }
}

class Nodecacheless{
    State state = new State();
    Nodecacheless parent = null;
    List<Nodecacheless> children = new ArrayList<>();
    Actioncacheless transition;

    public Nodecacheless(){}
    public Nodecacheless(State state, Nodecacheless parent, Actioncacheless transition){
        this.state = state;
        this.parent = parent;
        this.transition = transition;
    }

    public Nodecacheless getRandomChild(){
        Random rand = new Random();
        return children.get(rand.nextInt(children.size()));
    }

    public Nodecacheless getBestChild(){
        return Collections.max(
                children,
                Comparator.comparing(c -> c.state.winrate)
        );
    }
}

class Actioncacheless{
    public Type type;
    public PylosSphere sphere;
    public PylosLocation prevlocation;
    public PylosLocation nextlocation;
    public PylosGameIF game;
    public PylosGameState prevState;
    public PylosPlayerColor prevcolor;

    Actioncacheless(Type type, PylosSphere sphere, PylosLocation prev, PylosLocation next, PylosGameIF game, PylosGameState prevstate, PylosPlayerColor prevcolor){
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
                simulatorcacheless.moveSphere(sphere,nextlocation);
                break;
            case REMOVE_FIRST:
            case REMOVE_SECOND:
                simulatorcacheless.removeSphere(sphere);
                break;
            case PASS:
                simulatorcacheless.pass();
                break;
        }
    }

    void undo(){
        switch(type){
            case ADD:
                simulatorcacheless.undoAddSphere(sphere,prevState,prevcolor);
                break;
            case REMOVE_FIRST:
                simulatorcacheless.undoRemoveFirstSphere(sphere,prevlocation,prevState,prevcolor);
                break;
            case REMOVE_SECOND:
                simulatorcacheless.undoRemoveSecondSphere(sphere,prevlocation,prevState,prevcolor);
                break;
            case UPGRADE:
                simulatorcacheless.undoMoveSphere(sphere, prevlocation,prevState,prevcolor);
                break;
            case PASS:
                simulatorcacheless.undoPass(prevState,prevcolor);
                break;
        }
    }
}

