//
// EvoLudo Project
//
// Copyright 2010 Christoph Hauert
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// For publications in any form, you are kindly requested to attribute the
// author and project as follows:
//
//	Hauert, Christoph (<year>) EvoLudo Project, http://www.evoludo.org
//			(doi: <doi>[, <version>])
//
//	<doi>:	digital object identifier of the downloaded release (or the
//			most recent release if downloaded from github.com),
//	<year>:	year of release (or download), and
//	[, <version>]: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.graphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.evoludo.geom.Node3D;
import org.evoludo.graphics.AbstractGraph.Zooming;
import org.evoludo.simulator.ColorMap3D;
import org.evoludo.ui.ContextMenu;
import org.evoludo.ui.ContextMenuCheckBoxItem;
import org.evoludo.ui.ContextMenuItem;
import org.evoludo.ui.TrackballControls;
import org.evoludo.simulator.ColorMap;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Network;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.Network3D;
import org.evoludo.simulator.models.Model;
import org.evoludo.util.Formatter;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

import thothbot.parallax.core.client.AnimatedScene;
import thothbot.parallax.core.client.RenderingPanel;
import thothbot.parallax.core.client.context.Canvas3d;
import thothbot.parallax.core.client.events.Context3dErrorEvent;
import thothbot.parallax.core.client.events.Context3dErrorHandler;
import thothbot.parallax.core.shared.cameras.Camera;
import thothbot.parallax.core.shared.cameras.OrthographicCamera;
import thothbot.parallax.core.shared.cameras.PerspectiveCamera;
import thothbot.parallax.core.shared.core.Object3D;
import thothbot.parallax.core.shared.core.Raycaster;
import thothbot.parallax.core.shared.geometries.BoxGeometry;
import thothbot.parallax.core.shared.geometries.SphereGeometry;
import thothbot.parallax.core.shared.lights.AmbientLight;
import thothbot.parallax.core.shared.lights.PointLight;
import thothbot.parallax.core.shared.materials.LineBasicMaterial;
import thothbot.parallax.core.shared.materials.Material;
import thothbot.parallax.core.shared.materials.MeshLambertMaterial;
import thothbot.parallax.core.shared.math.Vector3;
import thothbot.parallax.core.shared.objects.Line;
import thothbot.parallax.core.shared.objects.Line.MODE;
import thothbot.parallax.core.shared.objects.Mesh;
import thothbot.parallax.core.shared.scenes.Scene;
import thothbot.parallax.plugins.effects.Anaglyph;
import thothbot.parallax.plugins.effects.Effect;
import thothbot.parallax.plugins.effects.Stereo;

/**
 * The graphical representation of network structures in 3D.
 *
 * @author Christoph Hauert
 */
public class PopGraph3D extends AbstractGraph implements Zooming, DoubleClickHandler, //
		Network.LayoutListener, Context3dErrorHandler {

	/**
	 * The structure of the population.
	 */
	protected Geometry geometry;

	/**
	 * The network representation of the population structure or {@code null} if not
	 * applicable.
	 */
	protected Network3DGWT network;

	protected RenderingPanel graph3DPanel;
	protected Pop3DScene graph3DScene;
	protected Camera graph3DCamera;
	protected ArrayList<Mesh> spheres;

	/**
	 * The flag to indicate whether camera needs to be reset. This is true after the
	 * geometry changed (but not after a reset).
	 */
	protected boolean resetCamera = true;

	/**
	 * The array of colors used for drawing the nodes of the graph.
	 * <p>
	 * <strong>Note:</strong> This is deliberately hiding
	 * {@link AbstractGraph#colors} because it serves the exact same purpose but is
	 * an array of type {@code MeshLambertMaterial[]} instead of {@code Color[]}.
	 */
	@SuppressWarnings("hiding")
	protected MeshLambertMaterial[] colors;
	protected LineBasicMaterial linkstyle;
	protected PointLight light;
	protected AmbientLight ambient;
	protected Label msgLabel;

	private boolean invalidated = true;

	/**
	 * The map for translating discrete traits into colors.
	 */
	protected ColorMap<MeshLambertMaterial> colorMap;

	/**
	 * The label of the graph.
	 */
	protected Label label;

	/**
	 * Create a graph for graphically visualizing the structure of a network (or
	 * population). Allocates the canvas and the label and retrieves the shared
	 * tooltip and context menu.
	 * 
	 * <h2>CSS Style Rules</h2>
	 * <dl>
	 * <dt>.evoludo-PopGraph3D</dt>
	 * <dd>the graph element.</dd>
	 * <dt>.evoludo-Label3D</dt>
	 * <dd>the graph label element.</dd>
	 * <dt>.evoludo-Message3D</dt>
	 * <dd>the message element (3D text).</dd>
	 * </dl>
	 * 
	 * @param controller the controller of this graph
	 * @param tag        the identifying tag
	 */
	public PopGraph3D(NodeGraphController controller, int tag) {
		super(controller, tag);
		setStylePrimaryName("evoludo-PopGraph3D");
		// PopGraph3D cannot use wrapper - transfer all widgets to graphPanel3D
		// (except canvas) and make this the wrapper
		graph3DPanel = new RenderingPanel();
		for (Widget widget : wrapper) {
			if (widget == canvas)
				continue;
			graph3DPanel.add(widget);
		}
		canvas = null;
		wrapper.clear();
		remove(wrapper);
		wrapper = graph3DPanel;
		// background color - apparently cannot be simply set using CSS
		graph3DPanel.setBackground(0x444444);
		graph3DPanel.setStylePrimaryName("evoludo-Canvas3D");
		graph3DPanel.addCanvas3dErrorHandler(this);
		graph3DScene = new Pop3DScene();
		graph3DPanel.setAnimatedScene(graph3DScene);
		label = new Label("Gugus");
		label.setStyleName("evoludo-Label3D");
		label.getElement().getStyle().setZIndex(1);
		label.setVisible(false);
		graph3DPanel.add(label);
		// adding message label on demand later on causes trouble...
		msgLabel = new Label("Gugus");
		msgLabel.setStyleName("evoludo-Message3D");
		msgLabel.setVisible(false);
		graph3DPanel.add(msgLabel);
		add(wrapper);
		element = graph3DPanel.getElement();
	}

	@Override
	public void activate() {
		super.activate();
		if (network != null)
			network.setLayoutListener(this);
		// lazy allocation of memory for colors
		if (geometry != null && (colors == null || colors.length != geometry.size))
			colors = new MeshLambertMaterial[geometry.size];
		// cannot yet start animation - kills scene
		// graph3DScene.run();
		doubleClickHandler = addDoubleClickHandler(this);
		// 3D graphs do not implement Shifting interface. Add mouse listeners here.
		mouseDownHandler = addMouseDownHandler(this);
		mouseUpHandler = addMouseUpHandler(this);
	}

	@Override
	public void deactivate() {
		graph3DScene.stop();
		super.deactivate();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <strong>Note:</strong> deliberately does <em>not</em> call super because no
	 * Canvas2D is needed for 3D graphs
	 */
	@Override
	public void alloc() {
		graph3DPanel.onResize();
	}

	/**
	 * Start the animation of the 3D scene.
	 * <p>
	 * <strong>Note:</strong> It is important to only run the animation as long as
	 * the graph is visible. Even when idling the animation consumes considerable
	 * CPU resources.
	 */
	public void animate() {
		graph3DScene.run();
	}

	/**
	 * Set the graph label to the string {@code msg} (no HTML formatting).
	 * 
	 * @param msg the text for the label of the graph
	 */
	public void setGraphLabel(String msg) {
		label.setText(msg);
		label.setVisible(msg != null && !msg.isEmpty());
	}

	/**
	 * Set the geometry backing the graph.
	 * 
	 * @param geometry the structure of the population
	 */
	public void setGeometry(Geometry geometry) {
		Geometry.Type type = (geometry == null ? null : geometry.getType());
		resetCamera = (this.geometry == null || this.geometry.isType(type));
		this.geometry = geometry;
		// geometry (and network) may be null for Model.ODE or Model.SDE
		if (geometry == null)
			return;
		setGraphLabel(geometry.getName());
		// update population
		network = (Network3DGWT) geometry.getNetwork3D();
	}

	/**
	 * Get the geometry backing the graph.
	 * 
	 * @return the structure of the population
	 */
	public Geometry getGeometry() {
		return geometry;
	}

	/**
	 * Set the map for translating trait values into colors.
	 * 
	 * @param colorMap the trait-to-colour map
	 */
	public void setColorMap(ColorMap<MeshLambertMaterial> colorMap) {
		this.colorMap = colorMap;
	}

	/**
	 * Get the map for translating trait values into colors.
	 * 
	 * @return the trait-to-colour map
	 */
	public ColorMap<MeshLambertMaterial> getColorModel() {
		return colorMap;
	}

	/**
	 * Get the color data for all nodes as an array.
	 * 
	 * @return the array of node colors
	 */
	public MeshLambertMaterial[] getData() {
		return colors;
	}

	/**
	 * Get the graphical 2D network representation of the graph represented by
	 * geometry.
	 * 
	 * @return the 2D network representation of this graph
	 */
	public Network3D getNetwork() {
		return network;
	}

	@Override
	public void reset() {
		super.reset();
		// geometry (and network) may be null for Model.ODE or Model.SDE
		if (geometry == null)
			return;
		network.reset();
		initUniverse();
	}

	/**
	 * Invalidate the network. This forces networks to be regenerated.
	 */
	public void invalidate() {
		// geometry (and network) may be null for Model.ODE or Model.SDE
		if (network != null)
			network.reset();
		clearMessage();
		invalidated = true;
	}

	@Override
	public synchronized void layoutProgress(double p) {
		displayMessage("Laying out network...  " + Formatter.formatPercent(p, 0) + " completed.");
	}

	@Override
	public synchronized void layoutUpdate() {
		layoutUniverse();
		paint();
	}

	@Override
	public synchronized void layoutComplete() {
		clearMessage();
		layoutUniverse();
		((NodeGraphController) controller).layoutComplete();
	}

	/**
	 * Get the canvas element.
	 * <p>
	 * <strong>Notes:</strong>
	 * <ul>
	 * <li>Before exporting the canvas to image/png the scene needs to be rendered.
	 * <li>For resizing just returning the canvas element is fine.
	 * <li>Attempts to render the scene of an inactive view triggers lots of WebGL
	 * warnings.
	 * </ul>
	 * 
	 * @param isRunning {@code true} if the graph is active (visible)
	 * @return the canvas element
	 */
	public CanvasElement getCanvasElement(boolean isRunning) {
		if (isRunning)
			graph3DScene.getRenderer().render(graph3DScene.getScene(), graph3DCamera);
		return graph3DScene.getCanvas().getCanvas();
	}

	@Override
	public void paint() {
		if (!isActive || spheres == null)
			return;
		if (invalidated)
			initUniverse();
		int k = 0;
		for (Mesh sphere : spheres)
			sphere.setMaterial(colors[k++]);
		tooltip.update();
	}

	/**
	 * Update the graph.
	 * 
	 * @param isNext {@code true} if the state has changed
	 */
	public void update(boolean isNext) {
		paint();
		network.doLayout(this);
	}

	@Override
	public void zoom(double zoom) {
		if (zoom <= 0.0)
			graph3DScene.zoom();
		else
			graph3DScene.zoom(1.0 / zoom);
	}

	/**
	 * Initializes the 3D universe. Depending on the backing geometry this either
	 * <ol>
	 * <li>shows a message, if no graphical representation is available, e.g. for 1D
	 * linear lattices.
	 * <li>shows lattice geometries.
	 * <li>initiates the generic layouting process for arbitrary network structures.
	 * </ol>
	 * 
	 * @see Network3D
	 */
	@SuppressWarnings("null")
	public void initUniverse() {
		// scene ready to be set up?
		if (graph3DScene.getScene() == null)
			return;
		// clear scene to avoid flashing
		graph3DScene.getScene().getChildren().clear();
		// reset camera to original position if graph changed
		if (resetCamera) {
			graph3DScene.resetCamera();
			resetCamera = false;
		}

		if (spheres == null)
			spheres = new ArrayList<Mesh>();
		spheres.clear();
		clearMessage();
		invalidated = false;

		Geometry.Type type = geometry.getType();
		boolean isHierarchy = (type == Geometry.Type.HIERARCHY);
		if (isHierarchy)
			type = geometry.subgeometry;
		// geometries that have special/fixed layout
		switch (type) {
			case VOID:
			case LINEAR:
				spheres = null;
				displayMessage("No representation for geometry!");
				return;
			case CUBE:
				int side, zdim;
				// NOVA settings
				if (geometry.size == 25000) {
					zdim = 10;
					side = 50;
				} else {
					side = (int) (Math.pow(geometry.size, 1.0 / 3.0) + 0.5);
					zdim = side;
				}
				double incr = (Network3D.UNIVERSE_RADIUS + Network3D.UNIVERSE_RADIUS) / side;
				double radius = Math.max(1.0, incr * 0.3);
				double shift = (side - 1) * 0.5 * incr;
				double zshift = (zdim - 1) * 0.5 * incr;
				int idx = 0;
				thothbot.parallax.core.shared.core.Geometry unit = new SphereGeometry(radius, 16, 12);
				double posk = -zshift;
				for (int k = 0; k < zdim; k++) {
					double posj = -shift;
					for (int j = 0; j < side; j++) {
						double posi = -shift;
						for (int i = 0; i < side; i++) {
							Mesh mesh = new Mesh(unit);
							mesh.setMaterial(colors[idx]);
							mesh.setName(Integer.toString(idx++));
							mesh.setPosition(new Vector3(posi, posj, posk));
							mesh.setMatrixAutoUpdate(false);
							mesh.updateMatrix();
							spheres.add(mesh);
							posi += incr;
						}
						posj += incr;
					}
					posk += incr;
				}
				break;
			case SQUARE:
				side = (int) Math.sqrt(geometry.size); // data.size does not seem to be set at this point
				incr = (Network3D.UNIVERSE_RADIUS + Network3D.UNIVERSE_RADIUS) / side;
				radius = Math.max(1.0, incr * 0.4);
				shift = (side - 1) * 0.5 * incr;
				idx = 0;
				unit = new BoxGeometry(1.75 * radius, 1.75 * radius, 1.75 * radius);
				int hLevels = 0;
				int[] hPeriods = null;
				int HIERARCHY_GAP = 8; // unit gap in units?
				// for hierarchical structures add gap between units
				if (isHierarchy) {
					hLevels = geometry.hierarchy.length - 1;
					hPeriods = new int[hLevels];
					hPeriods[0] = (int) Math.sqrt(geometry.hierarchy[hLevels]);
					int totGap = side / hPeriods[0] - 1;
					for (int i = 1; i < hLevels; i++) {
						hPeriods[i] = hPeriods[i - 1] * (int) Math.sqrt(geometry.hierarchy[hLevels - i]);
						totGap += side / hPeriods[i] - 1;
					}
					shift += totGap * HIERARCHY_GAP * 0.5;
				}
				double posj = -shift;
				for (int j = 0; j < side; j++) {
					if (isHierarchy && j > 0) {
						for (int l = 0; l < hLevels; l++) {
							if (j % hPeriods[l] != 0)
								break;
							posj += HIERARCHY_GAP;
						}
					}
					double posi = -shift;
					for (int i = 0; i < side; i++) {
						if (isHierarchy && i > 0) {
							for (int l = 0; l < hLevels; l++) {
								if (i % hPeriods[l] != 0)
									break;
								posi += HIERARCHY_GAP;
							}
						}
						Mesh mesh = new Mesh(unit);
						mesh.setName(Integer.toString(idx++));
						mesh.setPosition(new Vector3(posi, posj, 0));
						mesh.setMatrixAutoUpdate(false);
						mesh.updateMatrix();
						spheres.add(mesh);
						posi += incr;
					}
					posj += incr;
				}
				break;
			case HONEYCOMB:
				side = (int) Math.sqrt(geometry.size); // data.size does not seem to be set at this point
				double hincr = (Network3D.UNIVERSE_RADIUS + Network3D.UNIVERSE_RADIUS) / side;
				double hincr2 = hincr * 0.5;
				double vincr = hincr * 0.5 * Math.sqrt(3.0);
				radius = Math.max(1.0, hincr * 0.4);
				shift = (side - 0.5) * 0.5 * hincr;
				idx = 0;
				unit = new SphereGeometry(radius, 16, 12);
				posj = -(side - 1) * 0.5 * vincr;
				for (int j = 0; j < side; j++) {
					double posi = -shift + (j % 2) * hincr2;
					for (int i = 0; i < side; i++) {
						Mesh mesh = new Mesh(unit);
						mesh.setMaterial(colors[idx]);
						mesh.setName(Integer.toString(idx++));
						mesh.setPosition(new Vector3(posi, posj, 0));
						mesh.setMatrixAutoUpdate(false);
						mesh.updateMatrix();
						spheres.add(mesh);
						posi += hincr;
					}
					posj += vincr;
				}
				break;

			case TRIANGULAR:
				side = (int) Math.sqrt(geometry.size); // data.size does not seem to be set at this point
				int size2 = side / 2;
				vincr = (Network3D.UNIVERSE_RADIUS + Network3D.UNIVERSE_RADIUS) / side;
				double vincr2 = vincr * 0.5;
				hincr = vincr * Math.sqrt(3.0);
				hincr2 = hincr * 0.5;
				radius = Math.max(1.0, vincr * 0.4);
				shift = (size2 - 0.5) * 0.5 * hincr;
				double vshift = (side - 1.25) * 0.75 * vincr;
				idx = 0;
				unit = new SphereGeometry(radius, 16, 12);
				// even nodes
				posj = -vshift;
				for (int j = 0; j < side; j++) {
					double posi = -shift;
					for (int i = 0; i < size2; i++) {
						Mesh mesh = new Mesh(unit);
						mesh.setMaterial(colors[idx]);
						mesh.setName(Integer.toString(idx));
						mesh.setPosition(new Vector3(posi, posj, 0));
						mesh.setMatrixAutoUpdate(false);
						mesh.updateMatrix();
						spheres.add(mesh);
						idx += 2;
						posi += hincr;
					}
					posj += (1 + (j % 2)) * vincr;
				}
				// odd nodes
				idx = 1;
				posj = -vshift - vincr2;
				for (int j = 0; j < side; j++) {
					double posi = -shift + hincr2;
					for (int i = 0; i < size2; i++) {
						Mesh mesh = new Mesh(unit);
						mesh.setMaterial(colors[idx]);
						mesh.setName(Integer.toString(idx));
						mesh.setPosition(new Vector3(posi, posj, 0));
						mesh.setMatrixAutoUpdate(false);
						mesh.updateMatrix();
						spheres.add(idx, mesh);
						idx += 2;
						posi += hincr;
					}
					posj += (2 - (j % 2)) * vincr;
				}
				break;

			// case GENERIC:
			// case DYNAMIC:
			// and many more...
			default:
				// allocate elements of universe - place them later
				unit = new SphereGeometry(50, 16, 12);
				// NOTE: must rely on geometry.size (instead of network.nNodes) because network
				// may not yet have been properly
				// synchronized (Network.doLayoutPrep will take care of this)
				// No need to check whether network is null because ODE/SDE models
				// would never get here.
				for (int k = 0; k < geometry.size; k++) {
					Mesh mesh = new Mesh(unit);
					mesh.setMaterial(colors[k]);
					mesh.setName(Integer.toString(k));
					mesh.setMatrixAutoUpdate(false);
					spheres.add(mesh);
				}
				spheres.trimToSize();
				switch (network.getStatus()) {
					case ADJUST_LAYOUT:
					case NEEDS_LAYOUT:
						network.doLayout(this);
						break;
					case HAS_LAYOUT:
						layoutUniverse();
						break;
					case NO_LAYOUT:
					case LAYOUT_IN_PROGRESS:
					default:
				}
				return;
		}
		spheres.trimToSize();
		populateUniverse();
	}

	/**
	 * Called during an animated layouting process of Network3D.
	 */
	protected void layoutUniverse() {
		if (network.isStatus(Status.NO_LAYOUT))
			return; // nothing to do (lattice)
		// link nodes
		Node3D[] nodes = network.toArray();
		// place spheres
		int k = 0;
		for (Iterator<Mesh> i = spheres.iterator(); i.hasNext();) {
			Mesh mesh = i.next();
			Node3D node = nodes[k];
			Vector3 vec = mesh.getPosition();
			vec.setX(node.x);
			vec.setY(node.y);
			vec.setZ(node.z);
			double radius = 2.0 * node.r / Network3D.UNIVERSE_RADIUS;
			vec = mesh.getScale();
			vec.setX(radius);
			vec.setY(radius);
			vec.setZ(radius);
			mesh.updateMatrix();
			k++;
		}
		populateUniverse();
	}

	/**
	 * Adds the nodes, links, lights and camera to the scene.
	 */
	public void populateUniverse() {
		Scene scene = graph3DScene.getScene();
		scene.getChildren().clear();
		scene.add(new Object3D().add(spheres));
		scene.add(graph3DCamera);
		scene.add(ambient);
		thothbot.parallax.core.shared.core.Geometry lines = network.getLinks();
		if (lines != null) {
			// this does not work - seems to be a bug (works but does not update the number
			// of line segments drawn)
			// ((thothbot.parallax.core.shared.core.Geometry)links.getGeometry()).setVertices(lines);
			// links.getGeometry().setVerticesNeedUpdate(true);
			// geometry can only be attached to one Canvas3d - clone geometry if not network
			// founder
			linkstyle.setColor(geometry.isUndirected ? ColorMap3D.UNDIRECTED : ColorMap3D.DIRECTED);
			linkstyle.setVertexColors(Material.COLORS.VERTEX);
			Line links = new Line(lines, linkstyle, MODE.PIECES);
			links.setMatrixAutoUpdate(false);
			scene.add(links);
		}
		// IMPORTANT: The following lines are crucial for Safari (desktop and iPadOS). Safari  
		// requires special convincing to properly display lattices as well as animated networks.  
		// Chrome, Firefox and Safari (iOS) are all fine. In those cases the following lines are  
		// not needed but do no harm either.
		if (network.isStatus(Status.NO_LAYOUT) || network.isStatus(Status.HAS_LAYOUT)) {
			int k = 0;
			for (Mesh sphere : spheres)
				sphere.setMaterial(colors[k++]);
			graph3DPanel.forceLayout();
		}
		msgLabel.setText("");
		msgLabel.setVisible(true);
	}

	@Override
	public String getTooltipAt(int x, int y) {
		// no network may have been initialized (e.g. for ODE/SDE models)
		// when switching views the graph may not yet be ready to return
		// data for tooltips (colors == null)
		if (leftMouseButton || contextMenu.isVisible() || network == null || colors == null
				|| network.isStatus(Status.LAYOUT_IN_PROGRESS))
			return null;
		int node = findNodeAt(x, y);
		if (node < 0) {
			element.removeClassName("evoludo-cursorPointNode");
			return null;
		}
		element.addClassName("evoludo-cursorPointNode");
		return ((NodeGraphController) controller).getTooltipAt(this, node);
	}

	/**
	 * Helper field for determining which node has been hit by mouse or tap.
	 */
	Raycaster raycaster = new Raycaster();

	/**
	 * Return value if {@link #findNodeAt(int, int)} couldn't find a node at the
	 * mouse position.
	 */
	static final int FINDNODEAT_OUT_OF_BOUNDS = -1;

	/**
	 * Return value if {@link #findNodeAt(int, int)} isn't implemented for the
	 * particular backing geometry.
	 */
	static final int FINDNODEAT_UNIMPLEMENTED = -2;

	/**
	 * Find the index of the node at the location with coordinates {@code (x, y)}.
	 * 
	 * @param x the {@code x}-coordinate of the location
	 * @param y the {@code y}-coordinate of the location
	 * @return the index of the node
	 */
	public int findNodeAt(int x, int y) {
		if (hasMessage || network == null || network.isStatus(Status.LAYOUT_IN_PROGRESS))
			return FINDNODEAT_OUT_OF_BOUNDS;
		if (graph3DCamera == null)
			return FINDNODEAT_OUT_OF_BOUNDS;
		// disable picking in stereo mode. how should/could picking be implemented?
		if (effect instanceof Stereo)
			return FINDNODEAT_UNIMPLEMENTED;
		Vector3 vector = new Vector3(2.0 * x / graph3DPanel.getOffsetWidth() - 1.0,
				-2.0 * y / graph3DPanel.getOffsetHeight() + 1.0, -1).unproject(graph3DCamera);
		if (graph3DCamera instanceof PerspectiveCamera) {
			raycaster.set(graph3DCamera.getPosition(), vector.sub(graph3DCamera.getPosition()).normalize());
		} else {
			Vector3 dir = new Vector3(0, 0, -1).transformDirection(graph3DCamera.getMatrixWorld());
			raycaster.set(vector, dir);
		}
		List<Raycaster.Intersect> intersects = raycaster.intersectObjects(spheres, false);
		if (intersects.size() == 0)
			return FINDNODEAT_OUT_OF_BOUNDS;
		return Integer.parseInt(intersects.get(0).object.getName());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * No need to calculate bounds in 3D world.
	 */
	@Override
	protected boolean calcBounds() {
		return true;
	}

	@Override
	public void onMouseDown(MouseDownEvent event) {
		if (event.getNativeButton() == NativeEvent.BUTTON_LEFT) {
			// hides tooltips
			leftMouseButton = true;
		}
	}

	@Override
	public void onMouseUp(MouseUpEvent event) {
		leftMouseButton = false;
	}

	@Override
	public void onDoubleClick(DoubleClickEvent event) {
		// ignore if busy or invalid node
		int node = findNodeAt(event.getX(), event.getY());
		if (node >= 0 && !controller.isRunning()) {
			// population signals change back to us
			((NodeGraphController) controller).mouseHitNode(tag, node, event.isAltKeyDown());
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The graph reacts to different kinds of touches:
	 * <dl>
	 * <dt>short touch with two fingers ({@code &lt;250} msec)
	 * <dd>display context menu.
	 * <dt>single long touch ({@code &gt;250} msec) on a node
	 * <dd>display the tooltip.
	 * <dt>long touch with two fingers ({@code &gt;250} msec)
	 * <dd>initiates pinching zoom.
	 * <dt>double tap on a node
	 * <dd>change the strategy of the node, if applicable.
	 * </dl>
	 * 
	 * @see ContextMenu.Provider
	 * @see #populateContextMenuAt(ContextMenu, int, int)
	 */
	@Override
	public void onTouchStart(TouchStartEvent event) {
		JsArray<Touch> touches = event.getTouches();
		if (touches.length() > 1) {
			// more than one touch point
			return;
		}
		Touch touch = touches.get(0);
		Element ref = graph3DPanel.getCanvas().getElement();
		int x = touch.getRelativeX(ref);
		int y = touch.getRelativeY(ref);
		int node = findNodeAt(x, y);
		if (node < 0) {
			// no node touched
			tooltip.close();
			return;
		}
		if (Duration.currentTimeMillis() - touchEndTime < 250.0) {
			// double tap
			if (!controller.isRunning())
				((NodeGraphController) controller).mouseHitNode(tag, node); // population signals change back to us
			event.preventDefault();
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The number of touches on the graph changed.
	 */
	@Override
	public void onTouchEnd(TouchEndEvent event) {
		// note: rotation of scene through touch events is managed by
		// @see org.evoludo.simulator.web.gwt.TrackballControls
		if (event.getTouches().length() == 0) {
			// no more touches remaining - reset values
			touchEndTime = Duration.currentTimeMillis();
		}
	}

	/**
	 * The context menu item for animating the layouting process.
	 */
	private ContextMenuCheckBoxItem animateMenu;

	/**
	 * The context menu item for selecting parallel projection of the graph instead
	 * of the default perspective projection.
	 */
	private ContextMenuCheckBoxItem projectionMenu;

	/**
	 * The context menu item for selecting anaglyph projection of the 3D space for a
	 * reperesentation of the graph suitable for colored 3D glasses.
	 */
	private ContextMenuCheckBoxItem anaglyphMenu;

	/**
	 * The context menu item for selecting stereo projection of the 3D space for a
	 * virtual reality representation of the graph.
	 */
	private ContextMenuCheckBoxItem vrMenu;

	/**
	 * Helper variable for additional effects on the 3D view. This handles anaglyph
	 * and stereo projections.
	 */
	private Effect effect = null;

	/**
	 * The context menu item for rearranging networks through random shifts of node
	 * positions.
	 */
	private ContextMenuItem shakeMenu;

	/**
	 * The context menu for visually exploring (or debugging) the updating process.
	 */
	private ContextMenu debugSubmenu;

	/**
	 * The context menu item for updating the current node.
	 */
	private ContextMenuItem debugNodeMenu;

	/**
	 * The context menu item for attaching the debug submenu.
	 */
	private ContextMenuItem debugSubmenuTrigger;

	/**
	 * The flag to indicate whether the debug submenu is activated. For example,
	 * debugging does not make sense if the nodes refer to states of PDE
	 * calculations.
	 */
	private boolean isDebugEnabled = true;

	/**
	 * Set whether the debugging menu is enabled.
	 * 
	 * @param enabled {@code true} to enable debugging
	 */
	public void setDebugEnabled(boolean enabled) {
		isDebugEnabled = enabled;
	}

	@Override
	public void populateContextMenuAt(ContextMenu menu, int x, int y) {
		if (hasMessage) {
			// skip or disable context menu entries
			super.populateContextMenuAt(menu, x, y);
			return;
		}
		// process shake context menu
		if (shakeMenu == null) {
			shakeMenu = new ContextMenuItem("Shake", new Command() {
				@Override
				public void execute() {
					network.shake(PopGraph3D.this, 0.05);
				}
			});
		}
		menu.add(shakeMenu);
		switch (geometry.getType()) {
			case HIERARCHY:
				shakeMenu.setEnabled(geometry.subgeometry != Geometry.Type.SQUARE);
				break;
			// list of graphs that have static layout and hence do not permit shaking
			case LINEAR:
			case SQUARE:
			case CUBE:
			case HONEYCOMB:
			case TRIANGULAR:
			case VOID:
			case INVALID:
				shakeMenu.setEnabled(false);
				break;
			default:
				shakeMenu.setEnabled(true);
				break;
		}
		if (hasMessage)
			shakeMenu.setEnabled(false);

		// process animate context menu
		if (animateMenu == null) {
			animateMenu = new ContextMenuCheckBoxItem("Animate layout", new Command() {
				@Override
				public void execute() {
					network.toggleAnimateLayout();
				}
			});
		}
		animateMenu.setChecked(network == null ? false : network.doAnimateLayout());
		menu.add(animateMenu);
		animateMenu.setEnabled(!hasMessage);

		// process debug node update
		if (isDebugEnabled) {
			int debugNode = findNodeAt(x, y);
			if (debugNode >= 0) {
				if (debugSubmenu == null) {
					debugSubmenu = new ContextMenu(menu);
					debugNodeMenu = new ContextMenuItem("Update node @ -", new Command() {
						@Override
						public void execute() {
							((NodeGraphController) controller).updateNodeAt(PopGraph3D.this, debugNode);
						}
					});
					debugSubmenu.add(debugNodeMenu);
				}
				debugNodeMenu.setText("Update node @ " + debugNode);
				debugNodeMenu.setEnabled(((NodeGraphController) controller).isModelType(Model.Type.IBS));
				debugSubmenuTrigger = menu.add("Debug...", debugSubmenu);
			}
			if (debugSubmenuTrigger != null)
				debugSubmenuTrigger.setEnabled(!controller.isRunning());
		}

		// process perspective context menu
		menu.addSeparator();
		if (projectionMenu == null) {
			projectionMenu = new ContextMenuCheckBoxItem("Parallel projection", new Command() {
				@Override
				public void execute() {
					graph3DScene.setOrthographic(graph3DCamera instanceof PerspectiveCamera);
				}
			});
		}
		menu.add(projectionMenu);

		// process anaglyph context menu
		if (anaglyphMenu == null) {
			anaglyphMenu = new ContextMenuCheckBoxItem("Anaglyph 3D", new Command() {
				@Override
				public void execute() {
					// ensure stereo mode is exited
					if (effect != null) {
						boolean isAnaglyph = effect instanceof Anaglyph;
						graph3DPanel.getRenderer().deletePlugin(effect);
						effect = null;
						if (isAnaglyph)
							return;
					}
					effect = new Anaglyph(graph3DPanel.getRenderer(), graph3DScene.getScene());
				}
			});
		}
		menu.add(anaglyphMenu);

		// process virtual reality context menu
		if (vrMenu == null) {
			vrMenu = new ContextMenuCheckBoxItem("Virtual reality (β)", new Command() {
				@Override
				public void execute() {
					// ensure anaglyph mode is exited
					if (effect != null) {
						boolean isVR = effect instanceof Stereo;
						graph3DPanel.getRenderer().deletePlugin(effect);
						effect = null;
						if (isVR)
							return;
					}
					effect = new Stereo(graph3DPanel.getRenderer(), graph3DScene.getScene());
				}
			});
		}
		menu.add(vrMenu);

		// process anaglyph, stereo and perspective - note: anaglyph and stereo not
		// possible for parallel projection
		boolean isStereo = effect != null;
		boolean isOrthographic = graph3DCamera instanceof OrthographicCamera;
		if (isStereo && isOrthographic) {
			graph3DPanel.getRenderer().deletePlugin(effect);
			effect = null;
			isStereo = false;
		}
		projectionMenu.setChecked(isOrthographic);
		projectionMenu.setEnabled(!isStereo && !hasMessage);
		anaglyphMenu.setChecked(effect instanceof Anaglyph);
		anaglyphMenu.setEnabled(!isOrthographic && !hasMessage);
		vrMenu.setChecked(effect instanceof Stereo);
		vrMenu.setEnabled(!isOrthographic && !hasMessage);
		super.populateContextMenuAt(menu, x, y);
	}

	public boolean isFullscreenSupported() {
		return graph3DPanel.isSupportFullScreen();
	}

	@Override
	public void export(MyContext2d ctx) {
		ctx.save();
		ctx.scale(scale, scale);
		ctx.drawImage(getCanvasElement(graph3DScene.isRunning), 0, 0);
		ctx.restore();
	}

	/**
	 * The class for animating the 3D network structure.
	 */
	public class Pop3DScene extends AnimatedScene implements RequiresResize {

		/**
		 * The control for rotating and zooming the scene.
		 */
		TrackballControls control;

		/**
		 * The flag indicating whether animations are currently running.
		 */
		boolean isRunning = false;

		/**
		 * {@inheritDoc}
		 * <p>
		 * The scene should be defined already.
		 * 
		 * @see PopGraph3D#populateUniverse()
		 */
		@Override
		protected void onStart() {
			ambient = new AmbientLight(0x666666);
			linkstyle = new LineBasicMaterial();
			linkstyle.setOpacity(1.0);
			linkstyle.setLinewidth(2);
		}

		@Override
		public void stop() {
			super.stop();
			isRunning = false;
		}

		@Override
		public void run() {
			super.run();
			isRunning = true;
			positionCamera();
		}

		/**
		 * Checks if the scene is busy animating.
		 * 
		 * @return {@code true} if the animation is running.
		 */
		public boolean isRunning() {
			return isRunning;
		}

		/**
		 * Positions the camera. The camera position is shared between different views
		 * of the 3D graph, e.g. view of the strategies and view of fitnesses.
		 * 
		 * @see Network3DGWT#getWorldView()
		 */
		public void positionCamera() {
			if (geometry == null)
				return;
			Camera newWorldView = network.getWorldView();
			if (graph3DCamera != null && newWorldView == graph3DCamera)
				return;
			// new camera is needed if graph3DCamera==null or of different type than
			// newWorldView
			if (graph3DCamera == null
					|| ((graph3DCamera instanceof OrthographicCamera) != (newWorldView instanceof OrthographicCamera)))
				setOrtho(newWorldView instanceof OrthographicCamera);
			if (newWorldView != null) {
				// copy position of camera
				graph3DCamera.setPosition(newWorldView.getPosition());
				graph3DCamera.setQuaternion(newWorldView.getQuaternion());
				graph3DCamera.setUp(newWorldView.getUp());
				graph3DCamera.setScale(newWorldView.getScale());
				graph3DCamera.setRotation(newWorldView.getRotation());
			}
			initUniverse();
			paint();
		}

		/**
		 * Set the projection of the camera. If {@code setOrtho == true} the camera uses
		 * an orthographic (parallel) projection and a perspective projection otherwise.
		 * 
		 * @param setOrtho the flag to set an orthographic (parallel) projection for the
		 *                 camera
		 */
		public void setOrthographic(boolean setOrtho) {
			setOrtho(setOrtho);
			initUniverse();
			paint();
		}

		protected void resetCamera() {
			if (graph3DCamera == null)
				return;
			boolean isOrthographic = (graph3DCamera instanceof OrthographicCamera);
			graph3DCamera = null;
			setOrtho(isOrthographic);
		}
	
		/**
		 * Helper method to allocate, set, or change the camera for the scene.
		 * 
		 * @param doOrthographic {@code true} for an orthographic projection
		 */
		private void setOrtho(boolean doOrthographic) {
			if (doOrthographic) {
				// orthographic projection
				if (graph3DCamera instanceof OrthographicCamera)
					return;
				Canvas3d c3d = getCanvas();
				graph3DCamera = new OrthographicCamera(c3d.getWidth(), c3d.getHeight(), -10000, // near
						10000 // far
				);
			} else {
				if (graph3DCamera instanceof PerspectiveCamera)
					return;
				graph3DCamera = new PerspectiveCamera(60, // field of view
						getRenderer().getAbsoluteAspectRation(), // aspect ratio
						1, // near
						10000 // far
				);
			}
			// adding the light to the camera makes it static
			if (light == null) {
				light = new PointLight(0xffffff, 1, 0.0);
				light.getPosition().set(-1000, 1000, 1000);
			}
			graph3DCamera.add(light);
			graph3DCamera.getPosition().setZ(400);
			network.setWorldView(graph3DCamera);
			if (control != null)
				control.dispose();
			control = new TrackballControls(graph3DCamera, getCanvas());
			control.setPanSpeed(0.2);
			control.setDynamicDampingFactor(0.12);
		}

		@Override
		public void onResize() {
			if (control != null)
				control.onResize();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Update the view of the scene (perform rotations, zooming and panning, as
		 * requested) and render the scene.
		 */
		@Override
		protected void onUpdate(double duration) {
			if (hasMessage)
				return;
			control.update();
			getRenderer().render(getScene(), graph3DCamera);
		}

		/**
		 * Reset zoom level.
		 */
		public void zoom() {
			control.zoom();
		}

		/**
		 * Adjust zoom level by the factor {@code zoom}. Leave the center of the view in
		 * place.
		 * 
		 * @param zoom the new zoom level
		 */
		public void zoom(double zoom) {
			control.zoom(zoom);
		}
	}

	@Override
	public void onContextError(Context3dErrorEvent event) {
		logger.severe("Context3D error: " + event.getMessage());
	}

	@Override
	public boolean displayMessage(String msg) {
		if (msg == null || msg.isEmpty()) {
			clearMessage();
			return false;
		}
		hasMessage = true;
		g.save();
		g.setFont("12px sans-serif");
		msgLabel.setText(msg);
		msgLabel.setVisible(true);
		Style msgStyle = msgLabel.getElement().getStyle();
		msgStyle.setFontSize(12.0 * 0.666 * width / g.measureText(msg).getWidth(), Unit.PX);
		g.restore();
		return true;
	}

	@Override
	public void clearMessage() {
		if (hasMessage) {
			msgLabel.setVisible(false);
			hasMessage = false;
		}
	}
}