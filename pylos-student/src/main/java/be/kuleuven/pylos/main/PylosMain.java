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
import be.kuleuven.pylos.player.student.StudentPlayerBestFitWithCache;
//import be.kuleuven.pylos.player.student.StudentPlayerNegaMax;
import be.kuleuven.pylos.player.student.StudentPlayerBestFit;
import be.kuleuven.pylos.player.student.StudentPlayerRandomFit;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.xy.*;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;


import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class PylosMain {

	public PylosMain() {

	}

	public void startPerformanceBattles() {
		Random random = new Random(0);
		PylosPlayer[] players = new PylosPlayer[]{new PylosPlayerBestFit(), new PylosPlayerMiniMax(2), new PylosPlayerMiniMax(5), new PylosPlayerMiniMax(8)};

		int[] wins = new int[players.length];
		for (int i = 0; i < players.length; i++) {
			PylosPlayer player = new StudentPlayerBestFitWithCache();
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

	public void startBattlesTesting(){
		//final XYSeries data = new XYSeries( "testdata" );
		DefaultXYZDataset dataset = new DefaultXYZDataset();
		int diepte = 4;
		 //PylosPlayerBestFit();
		for (int i = 4; i < 11; i++) {
			double[][] data = new double[3][11];
			for (int j = 0; j < 11; j++) {
				double explorationparameter = 0.5 + j/10;
				PylosPlayer playerDark = new PylosPlayerMiniMax(i);
				PylosPlayer playerLight = new StudentPlayerBestFit(explorationparameter);
				double win = 100* Battle.play(playerLight, playerDark, 100, false)[0];
				System.out.println("("+i+","+j+") -> "+win);
				data[0][j] = i;
				data[1][j] = 0.5 + j/10;
				data[2][j] = win;
			}
			dataset.addSeries("Series" + i, data);
		}
		//JFreeChart xylineChart = createChart(dataset);

		int width = 640;  // Width of the image
		int height = 480; // Height of the image
		File XYChart = new File( "XYLineChart.jpeg" );
		JFrame f = new JFrame("Grafiek");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ChartPanel chartPanel = new ChartPanel(createChart(dataset)) {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(640, 480);
			}
		};
		try {
			ChartUtilities.saveChartAsJPEG( XYChart, chartPanel.getChart(), width, height);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		chartPanel.setMouseZoomable(true, false);
		f.add(chartPanel);
		f.pack();
		f.setLocationRelativeTo(null);
		f.setVisible(true);
		System.out.println("Klaar maken grafieken.");
	}

	private static JFreeChart createChart(XYDataset dataset) {
		NumberAxis xAxis = new NumberAxis("Diepte MiniMax tegenstander");
		NumberAxis yAxis = new NumberAxis("Waarde exploration parameter");
		XYPlot plot = new XYPlot(dataset, xAxis, yAxis, null);
		XYBlockRenderer r = new XYBlockRenderer();
		SpectrumPaintScale ps = new SpectrumPaintScale(0,100);
		r.setPaintScale(ps);
		r.setBlockHeight(1.0f);
		r.setBlockWidth(1.0f);
		plot.setRenderer(r);
		//String titel = "Tests tegen "+tegenstander+"("+diepte+")";
		String titel = "MCTS Tests";
		JFreeChart chart = new JFreeChart(titel,
				JFreeChart.DEFAULT_TITLE_FONT, plot, false);
		NumberAxis scaleAxis = new NumberAxis("Winrate in %");
		scaleAxis.setAxisLinePaint(Color.white);
		scaleAxis.setTickMarkPaint(Color.white);
		PaintScaleLegend legend = new PaintScaleLegend(ps, scaleAxis);
		legend.setSubdivisionCount(128);
		legend.setAxisLocation(AxisLocation.TOP_OR_RIGHT);
		legend.setPadding(new RectangleInsets(10, 10, 10, 10));
		legend.setStripWidth(20);
		legend.setPosition(RectangleEdge.RIGHT);
		legend.setBackgroundPaint(Color.WHITE);
		chart.addSubtitle(legend);
		chart.setBackgroundPaint(Color.white);
		return chart;
	}

	private static class SpectrumPaintScale implements PaintScale {

		private static final float H1 = 0f;
		private static final float H2 = 1f;
		private final double lowerBound;
		private final double upperBound;

		public SpectrumPaintScale(double lowerBound, double upperBound) {
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
		}

		@Override
		public double getLowerBound() {
			return lowerBound;
		}

		@Override
		public double getUpperBound() {
			return upperBound;
		}

		@Override
		public Paint getPaint(double value) {
			float scaledValue = (float) (value / (getUpperBound() - getLowerBound()));
			float scaledH = H1 + scaledValue * (H2 - H1);
			return Color.getHSBColor(scaledH, 1f, 1f);
		}
	}

	//PC ROBIN: exparameter 0.9 maxthinkingtime 50, exparameter 0.95 maxthinkingtime 40
	public void startBattle() {
		PylosPlayer playerLight = new StudentPlayerBestFit(0.95);// new StudentPlayerTest(10,2); new StudentPlayerNegaMax(10,2); StudentPlayerBestFitWithoutCache(0.95);
		PylosPlayer playerDark = new StudentPlayerBestFit(1.1); // PylosPlayerMiniMax(1); PylosPlayerBestFit()
		Battle.play(playerLight, playerDark, 100);
	}

	public static void main(String[] args) {

		/* !!! vm argument !!! -ea */

//		new PylosMain().startSingleGame();
		new PylosMain().startBattle();
//		new PylosMain().startBattlesTesting();

	}

}
