package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.player.PylosPlayer;
import be.kuleuven.pylos.player.PylosPlayerFactory;
import be.kuleuven.pylos.player.PylosPlayerType;

/**
 * Created by Jan on 20/02/2015.
 */
public class PlayerFactoryStudent extends PylosPlayerFactory {

	public PlayerFactoryStudent() {
		super("Student");
	}

	@Override
	protected void createTypes() {

		/* example */
		add(new PylosPlayerType("Student") {
			@Override
			public PylosPlayer create() {
				return new StudentPlayer();
			}
		});

		add(new PylosPlayerType("Student - Random") {
			@Override
			public PylosPlayer create() {
				return new StudentPlayerRandomFit();
			}
		});

		add(new PylosPlayerType("Student - Best Fit") {
			@Override
			public PylosPlayer create() {
				return new StudentPlayerBestFitWithCache();
			}
		});

		add(new PylosPlayerType("Student - Test") {
			@Override
			public PylosPlayer create() {
				return new StudentPlayerTest(4,9);
			}
		});

		add(new PylosPlayerType("Student - Cacheless") {
			@Override
			public PylosPlayer create() {
				return new StudentPlayerBestFit();
			}
		});
	}
}
