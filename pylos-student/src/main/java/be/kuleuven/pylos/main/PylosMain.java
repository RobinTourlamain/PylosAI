package be.kuleuven.pylos.main;

import be.kuleuven.pylos.battle.Battle;
import be.kuleuven.pylos.game.PylosBoard;
import be.kuleuven.pylos.game.PylosGame;
import be.kuleuven.pylos.game.PylosGameObserver;
import be.kuleuven.pylos.player.PylosPlayer;
import be.kuleuven.pylos.player.PylosPlayerObserver;
import be.kuleuven.pylos.player.codes.PylosPlayerBestFit;
import be.kuleuven.pylos.player.codes.PylosPlayerMiniMax;
import be.kuleuven.pylos.player.codes.PylosPlayerRandomFit;
import be.kuleuven.pylos.player.student.StudentPlayerBestFit;
import be.kuleuven.pylos.player.student.StudentPlayerRandomFit;

import java.util.Random;

public class PylosMain {

	public PylosMain() {

	}

	public void startPerformanceBattles() {
		Random random = new Random(0);
		PylosPlayer[] players = new PylosPlayer[]{new PylosPlayerBestFit(), new PylosPlayerMiniMax(2), new PylosPlayerMiniMax(5), new PylosPlayerMiniMax(8)};

		int[] wins = new int[players.length];
		for (int i = 0; i < players.length; i++) {
			PylosPlayer player = new StudentPlayerBestFit();
			PylosPlayer playerDark = players[i];
			double[] results = Battle.play(player, playerDark, 1000);
			wins[i] = (int) Math.round(results[0] * 100);
		}

		for (int win : wins) {
			System.out.print(win + "\t");
		}
	}

	public void startSingleGame() {

		Random random = new Random(0);

		PylosPlayer randomPlayerCodes = new PylosPlayerRandomFit();
//		PylosPlayer randomPlayerCodes = new PylosPlayerMiniMax();
		PylosPlayer randomPlayerStudent = new StudentPlayerRandomFit();

		PylosBoard pylosBoard = new PylosBoard();
		PylosGame pylosGame = new PylosGame(pylosBoard, randomPlayerCodes, randomPlayerStudent, random, PylosGameObserver.CONSOLE_GAME_OBSERVER, PylosPlayerObserver.NONE);

		pylosGame.play();
	}

	public void startBattle() {
		PylosPlayer playerLight = new StudentPlayerBestFit();
		PylosPlayer playerDark = new PylosPlayerRandomFit();
		Battle.play(playerLight, playerDark, 100);
	}

	public static void main(String[] args) {

		/* !!! vm argument !!! -ea */

//		new PylosMain().startSingleGame();
		new PylosMain().startBattle();

	}

}
