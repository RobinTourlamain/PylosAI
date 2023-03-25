package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.*;
import static be.kuleuven.pylos.player.student.StudentPlayerBestFitWithoutCache.simulatorcacheless;

public class StudentPlayerBestFitWithoutCache extends PylosPlayer{
    public static PylosGameSimulator simulatorcacheless;
    final long MAX_TIME_PER_MOVE = 50;
    public boolean isfirstmove = true;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        init(game.getState(), board);
        Actioncacheless next = null;

        //eerste move ergens in midden
        if(isfirstmove){ //board.getNumberOfSpheresOnBoard() <= 3 && simulator.getState() == PylosGameState.MOVE
            for(int x = 1; x<=2; x++){
                for(int y = 1; y<=2; y++){
                    if(board.getBoardLocation(x, y,0).isUsable()){
                        next = new Actioncacheless(Type.ADD, board.getReserve(this), board, null, board.getBoardLocation(x, y,0), game, PylosGameState.MOVE, this.PLAYER_COLOR);
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
        Nodecacheless root = new Nodecacheless();
        List<Nodecacheless> children = new ArrayList<>();
        for(Actioncacheless action : generateAllActions(game, board, this)){
            Nodecacheless newnode = new Nodecacheless(new State(), root, action);
            children.add(newnode);
        }
        root.children = children;

        int iterations = 0;
        long endtime = MAX_TIME_PER_MOVE + System.currentTimeMillis();

        while(System.currentTimeMillis() < endtime){
            Nodecacheless promising = selectPromising(root);

            if(simulatorcacheless.getState() != PylosGameState.COMPLETED){
                expandNode(promising, game, board);
            }

            Nodecacheless toexplore = promising;
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
//            backPropagation(toexplore, endresult);
            iterations++;
        }

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
        return ((double) winrate / (double) nodevisits) + 1.41 * Math.sqrt(Math.log(parentvisits) / (double) nodevisits);
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
        if(simulatorcacheless.getColor() == this.PLAYER_COLOR){
            player = this;
        }
        else{
            player = this.OTHER;
        }

        if(simulatorcacheless.getState() != PylosGameState.COMPLETED && simulatorcacheless.getState() != PylosGameState.DRAW) {
            List<Actioncacheless> actions = generateAllActions(game, board, player);
            Random rand = new Random();
            Actioncacheless next = actions.get(rand.nextInt(actions.size()));
            next.simulate();
            PylosPlayerColor result =  randomPlay(game, board, depth);
            next.undo();
            return result;
        }else if(simulatorcacheless.getState() == PylosGameState.COMPLETED){
            return simulatorcacheless.getWinner();
        }else{
            return null;
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

    public void backPropagation(Nodecacheless node, PylosPlayerColor result){
        Nodecacheless current = node;
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
                                actions.add(new Actioncacheless(Type.UPGRADE, sphere, board, sphere.getLocation(), location, game, PylosGameState.MOVE, player.PLAYER_COLOR));
                            }
                        }
                    }
                }

                //alle acties met reserve spheres
                PylosSphere myReserveSphere = board.getReserve(player);
                for(PylosLocation location : locations){
                    if(myReserveSphere.canMoveTo(location)){
                        actions.add(new Actioncacheless(Type.ADD, myReserveSphere, board, null, location, game, PylosGameState.MOVE, player.PLAYER_COLOR));
                    }
                }
                break;
            case REMOVE_FIRST:
                for(PylosSphere sphere: board.getSpheres(player)){
                    if(sphere.canRemove()){
                        actions.add(new Actioncacheless(Type.REMOVE_FIRST, sphere, board, sphere.getLocation(), null, game, PylosGameState.REMOVE_FIRST, player.PLAYER_COLOR));
                    }
                }
                break;
            case REMOVE_SECOND:
                for(PylosSphere sphere: board.getSpheres(player)){
                    if(sphere.canRemove()){
                        actions.add(new Actioncacheless(Type.REMOVE_SECOND, sphere, board, sphere.getLocation(), null, game, PylosGameState.REMOVE_SECOND, player.PLAYER_COLOR));
                    }
                }
                actions.add(new Actioncacheless(Type.PASS, null, board, null, null, game, PylosGameState.REMOVE_SECOND, player.PLAYER_COLOR));
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
    public PylosBoard board;
    public PylosLocation prevlocation;
    public PylosLocation nextlocation;
    public PylosGameIF game;
    public PylosGameState prevState;
    public PylosPlayerColor prevcolor;

    Actioncacheless(Type type, PylosSphere sphere, PylosBoard board, PylosLocation prev, PylosLocation next, PylosGameIF game, PylosGameState prevstate, PylosPlayerColor prevcolor){
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

