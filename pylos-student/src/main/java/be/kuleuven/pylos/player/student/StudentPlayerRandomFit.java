package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.PylosBoard;
import be.kuleuven.pylos.game.PylosGameIF;
import be.kuleuven.pylos.game.PylosLocation;
import be.kuleuven.pylos.game.PylosSphere;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Ine on 5/05/2015.
 */
public class StudentPlayerRandomFit extends PylosPlayer{

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        /* add a reserve sphere to a feasible random location */
        PylosSphere reserve = board.getReserve(this);
        List<PylosLocation> usable = new ArrayList<>();
        for(PylosLocation location : board.getLocations()){
            if(location.isUsable()){
                usable.add(location);
            }
        }

        PylosLocation destination = usable.get(0);
        game.moveSphere(reserve,destination);
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
		/* removeSphere a random sphere */
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
		/* always pass */
        game.pass();
    }
}
