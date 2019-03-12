/*
 * (C) Copyright 2005-2011, Gregor Heinrich (gregor :: arbylon : net) \
 * (This file is part of the knowceans-ilda experimental software package
 */
/*
 * knowceans-ilda is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 3 of the License, or (at your option) 
 * any later version.
 */
/*
 * knowceans-ilda is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
/*
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package cc.mallet.topics.tui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.knowceans.util.Vectors;

/**
 * MatrixCorpus does two things: Generating a corpus for MxN topics that can be
 * represented as rows and columns in a matrix, and displaying that matrix from
 * given nkt.
 * 
 * @author gregor
 * 
 */
public class TopicMatrixPanel extends JPanel {

	public boolean doLog;
	public boolean doSort;
	public boolean doNormalise;

	private static final long serialVersionUID = 1L;
	double[][] matrix = null;
	private int K;
	private double max;
	private int PIX = 20;

	/**
	 * create a topic matrix panel
	 * 
	 * @param width
	 * @param height
	 * @param K
	 *            number of topics (squares are K elements wide)
	 * @param pix
	 *            pixel width for each element / term
	 */
	public TopicMatrixPanel(int width, int height, int K, int pix) {
		this("Visual Matrix", width, height, K);
		PIX = pix;
	}

	public TopicMatrixPanel(String title, int width, int height, int K) {
		this.K = K;
		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(this);
		setPreferredSize(new Dimension(width, height));
		frame.setSize(width, height);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.addKeyListener(new KeyListener() {

			public void keyTyped(KeyEvent e) {
				char key = e.getKeyChar();
				if (key == 'n') {
					// TODO: handle normalisation
					// doNormalise = !doNormalise;
				}
				if (key == 'l') {
					// TODO: handle log values
					// doLog = !doLog;
				}
				if (key == '1') {
					PIX--;
					if (PIX == 0) {
						PIX = 1;
					}
				}
				if (key == '2') {
					PIX++;
				}
				repaint();
			}

			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
			}
		});

	}

	public void paint(Graphics g) {
		int w = getWidth();
		int h = getHeight();
		BufferedImage bufferedImage = new BufferedImage(w, h,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bufferedImage.createGraphics();
		// g2d.setColor(Color.white);
		g2d.setColor(Color.lightGray);
		g2d.fillRect(0, 0, w, h);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_DITHERING,
				RenderingHints.VALUE_DITHER_ENABLE);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g2d.setColor(new Color(150, 150, 150));

		RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF);
		rh.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		rh.put(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHints(rh);

		if (matrix == null) {
			return;
		}

		int xoffset = 10;
		int yoffset = 10;
		int K2 = K / 2;
		// we don't use K to handle growing topics in ILDA
		for (int k = 0; k < matrix.length; k++) {
			// check if empty topic
			if (matrix[k][0] != Double.NEGATIVE_INFINITY) {
				g2d.setColor(Color.red);
				for (int i = 0; i < K2; i++) {
					for (int j = 0; j < K2; j++) {
						g2d.drawRect(xoffset, yoffset, PIX * K2, PIX * K2);
						float u = 0;
						int k_x = i * K2 + j;
						System.out.println("k=" + k + " k_x=" + k_x);
						try {
							u = (float) ((float) matrix[k][k_x]);
						} catch (Exception e) {
							e.printStackTrace();
							System.out.println("i=" + i + " j=" + j);
						}
						g2d.setColor(new Color(u, u, u));
						g2d.fillRect(xoffset + i * PIX, yoffset + PIX * j, PIX,
								PIX);
					}
				}
			}
			xoffset += PIX * K2 + 10;
			if (xoffset + PIX * K2 + 10 >= getWidth()) {
				yoffset += PIX * K2 + 10;
				xoffset = 10;
			}
		}
		g2d = (Graphics2D) g;
		g2d.drawImage(bufferedImage, null, 0, 0);
	}

	public void setTopics(int[][] nkt) {
		if (matrix == null || nkt.length > matrix.length) {
			matrix = new double[nkt.length][nkt[0].length];
		}
		for (int k = 0; k < nkt.length; k++) {
			max = 0;
			for (int t = 0; t < nkt[0].length; t++) {
				matrix[k][t] = doLog ? Math.log(1 + nkt[k][t]) : nkt[k][t];
				max = Math.max(matrix[k][t], max);
			}
			if (doNormalise) {
				double sum = Vectors.sum(matrix[k]);
				Vectors.mult(matrix[k], 1 / sum * 255);
			}
			if (max == 0) {
				matrix[k][0] = Double.NEGATIVE_INFINITY;
			} else {
				if (doLog)
					max = Math.log(1 + max);
				Vectors.mult(matrix[k], 1. / max);
			}
		}
		repaint();
	}

	public void setTopics(List<int[]> nkt) {
		// has matrix size increased?
		if (matrix != null && nkt.size() > matrix.length) {
			matrix = null;
		}
		setTopics(nkt.toArray(new int[0][0]));
	}
}
