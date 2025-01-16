//
// EvoLudo Project
//
// Copyright 2010-2025 Christoph Hauert
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
//	Hauert, Christoph (<year>) EvoLudo Project, https://www.evoludo.org
//			(doi: 10.5281/zenodo.14591549 [, <version>])
//
//	<year>:    year of release (or download), and
//	<version>: optional version number (as reported in output header
//			or GUI console) to simplify replication of reported results.
//
// The formatting may be adjusted to comply with publisher requirements.
//

package org.evoludo.graphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.evoludo.geom.Node3D;
import org.evoludo.simulator.ColorMap3D;
import org.evoludo.simulator.Geometry;
import org.evoludo.simulator.Network.Status;
import org.evoludo.simulator.Network3D;
import org.evoludo.simulator.modules.Module;
import org.evoludo.ui.TrackballControls;

import com.google.gwt.core.client.Duration;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

import thothbot.parallax.core.client.AnimatedScene;
import thothbot.parallax.core.client.RenderingPanel;
import thothbot.parallax.core.client.context.Canvas3d;
import thothbot.parallax.core.client.events.Context3dErrorEvent;
import thothbot.parallax.core.client.events.Context3dErrorHandler;
import thothbot.parallax.core.client.renderers.WebGLRenderer;
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
 * The graphical representation of network structures in 3D. The 3D graph is
 * based on the <a href="http://www.parallax3d.org/">Parallax 3D</a> library.
 * The graph is displayed in a {@link RenderingPanel} which is a GWT widget
 * that wraps the Parallax 3D library.
 * <p>
 * The graph is interactive and allows the user to zoom and rotate the view. The
 * user can change the state of nodes by by double-clicking on them. The graph
 * can be exported in PNG or SVG graphics formats.
 *
 * 
 * @author Christoph Hauert
 */
public class PopGraph3D extends GenericPopGraph<MeshLambertMaterial, Network3DGWT> implements Context3dErrorHandler {

	/**
	 * The panel for rendering the 3D graph.
	 */
	RenderingPanel graph3DPanel;

	/**
	 * The 3D scene of the graph.
	 */
	Pop3DScene graph3DScene;

	/**
	 * The camera of the 3D graph.
	 */
	Camera graph3DCamera;

	/**
	 * The colors of the nodes.
	 */
	ArrayList<Mesh> spheres = new ArrayList<>();

	/**
	 * The line style for the links.
	 */
	LineBasicMaterial linkstyle;

	/**
	 * The directed light source illuminating the scene.
	 */
	PointLight light;

	/**
	 * The ambient light source illuminating the scene.
	 */
	AmbientLight ambient;

	/**
	 * The label for displaying messages.
	 */
	Label msgLabel;

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
	 * @param module     the module backing the graph
	 */
	public PopGraph3D(PopGraphController controller, Module module) {
		super(controller, module);
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
		// background color - apparently cannot be simply set using CSS
		graph3DPanel.setBackground(0x444444);
		graph3DPanel.setStylePrimaryName("evoludo-Canvas3D");
		graph3DPanel.addCanvas3dErrorHandler(this);
		graph3DScene = new Pop3DScene();
		graph3DPanel.setAnimatedScene(graph3DScene);
		label.setStyleName("evoludo-Label3D");
		// adding message label on demand later on causes trouble...
		msgLabel = new Label("Gugus");
		msgLabel.setStyleName("evoludo-Message3D");
		msgLabel.setVisible(false);
		graph3DPanel.add(msgLabel);
		add(graph3DPanel);
		element = graph3DPanel.getElement();
		wrapper = graph3DPanel;
	}

	@Override
	public void activate() {
		super.activate();
		// lazy allocation of memory for colors
		// ok to allocate data only here because no 3D view has history
		if (geometry != null && (data == null || data.length != geometry.size)) {
			data = new MeshLambertMaterial[geometry.size];
			// allocate one entry to be able to deduce the type of the array
			// in generic methods (see e.g. getLeafColor(...) in NetDyn)
			data[0] = new MeshLambertMaterial();
		}
		// cannot yet start animation - kills scene
		// graph3DScene.run();
		// 3D graphs do not implement Shifting interface. Add mouse listeners here.
		mouseDownHandler = addMouseDownHandler(this);
		mouseUpHandler = addMouseUpHandler(this);
		if (graph3DPanel.getRenderer() != null)
			graph3DScene.run();
	}

	@Override
	public void deactivate() {
		graph3DScene.stop();
		super.deactivate();
	}

	@Override
	public boolean paint(boolean force) {
		if (super.paint(force))
			return true;
		int k = 0;
		for (Mesh sphere : spheres)
			sphere.setMaterial(data[k++]);
		return false;
	}

	@Override
	protected void drawLattice() {
		Geometry.Type type = geometry.getType();
		boolean isHierarchy = (type == Geometry.Type.HIERARCHY);
		if (isHierarchy)
			type = geometry.subgeometry;
		// geometries that have special/fixed layout
		switch (type) {
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
				initUniverse(new BoxGeometry(1.75 * radius, 1.75 * radius, 1.75 * radius));
				Iterator<Mesh> meshes = spheres.iterator();
				double posk = -zshift;
				for (int k = 0; k < zdim; k++) {
					double posj = -shift;
					for (int j = 0; j < side; j++) {
						double posi = -shift;
						for (int i = 0; i < side; i++) {
							Mesh mesh = meshes.next();
							mesh.setPosition(new Vector3(posi, posj, posk));
							mesh.updateMatrix();
							posi += incr;
						}
						posj += incr;
					}
					posk += incr;
				}
				break;
			case SQUARE_NEUMANN:
			case SQUARE_MOORE:
			case SQUARE:
				side = (int) Math.sqrt(geometry.size); // data.size does not seem to be set at this point
				incr = (Network3D.UNIVERSE_RADIUS + Network3D.UNIVERSE_RADIUS) / side;
				radius = Math.max(1.0, incr * 0.4);
				shift = (side - 1) * 0.5 * incr;
				int hLevels = 0;
				int[] hPeriods = new int[0];
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
				initUniverse(new BoxGeometry(1.75 * radius, 1.75 * radius, 1.75 * radius));
				meshes = spheres.iterator();
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
						Mesh mesh = meshes.next();
						mesh.setPosition(new Vector3(posi, posj, 0));
						mesh.updateMatrix();
						posi += incr;
					}
					posj += incr;
				}
				break;
			case SQUARE_NEUMANN_2ND:
				side = (int) Math.sqrt(geometry.size); // data.size does not seem to be set at this point
				incr = (Network3D.UNIVERSE_RADIUS + Network3D.UNIVERSE_RADIUS) / side;
				radius = Math.max(1.0, incr * 0.4);
				shift = (side - 1) * 0.5 * incr;
				initUniverse(new BoxGeometry(1.75 * radius, 1.75 * radius, 1.75 * radius));
				meshes = spheres.iterator();
				posj = -shift;
				double posz = 0.4375 * radius;
				for (int j = 0; j < side; j++) {
					double posi = -shift;
					for (int i = 0; i < side; i++) {
						Mesh mesh = meshes.next();
						mesh.setPosition(new Vector3(posi, posj, posz));
						mesh.updateMatrix();
						posi += incr;
						posz = -posz;
					}
					posz = -posz;
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
				initUniverse(new SphereGeometry(radius, 16, 12));
				meshes = spheres.iterator();
				posj = -(side - 1) * 0.5 * vincr;
				for (int j = 0; j < side; j++) {
					double posi = -shift + (j % 2) * hincr2;
					for (int i = 0; i < side; i++) {
						Mesh mesh = meshes.next();
						mesh.setPosition(new Vector3(posi, posj, 0));
						mesh.updateMatrix();
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
				initUniverse(new SphereGeometry(radius, 16, 12));
				// initUniverse(new TetrahedronGeometry(radius * 2, 0));
				meshes = spheres.iterator();
				// even nodes
				posj = -vshift;
				for (int j = 0; j < side; j++) {
					double posi = -shift;
					for (int i = 0; i < size2; i++) {
						Mesh mesh = meshes.next();
						mesh.setPosition(new Vector3(posi, posj, 0));
						mesh.updateMatrix();
						posi += hincr;
					}
					posj += (1 + (j % 2)) * vincr;
				}
				// odd nodes
				posj = -vshift - vincr2;
				for (int j = 0; j < side; j++) {
					double posi = -shift + hincr2;
					for (int i = 0; i < size2; i++) {
						Mesh mesh = meshes.next();
						mesh.setPosition(new Vector3(posi, posj, 0));
						mesh.updateMatrix();
						posi += hincr;
					}
					posj += (2 - (j % 2)) * vincr;
				}
				break;
			default:
				displayMessage("No representation for " + type.getTitle() + "!");
				break;
		}
		drawUniverse();
	}

	/**
	 * Initialize the universe of the 3D graph. Allocate the spheres and set their
	 * material (colour).
	 * 
	 * @param unit the geometry of the unit (sphere) representing a node
	 */
	protected void initUniverse(thothbot.parallax.core.shared.core.Geometry unit) {
		spheres.clear();
		// allocate elements of universe - place them later
		// NOTE: must rely on geometry.size (instead of network.nNodes) because network
		// may not yet have been properly
		// synchronized (Network.doLayoutPrep will take care of this)
		// No need to check whether network is null because ODE/SDE models
		// would never get here.
		for (int k = 0; k < geometry.size; k++) {
			Mesh mesh = new Mesh(unit);
			mesh.setMaterial(data[k]);
			mesh.setName(Integer.toString(k));
			mesh.setMatrixAutoUpdate(false);
			spheres.add(mesh);
		}
		invalidated = false;
	}

	@Override
	protected void drawNetwork() {
		// if (!network.isStatus(Status.HAS_LAYOUT) || geometry.isDynamic)
		// network.doLayout(this);
		if (invalidated) {
			initUniverse(new SphereGeometry(50, 16, 12));
			if (network.isStatus(Status.HAS_LAYOUT))
				network.finishLayout(); // add links
		}
		// link nodes
		Node3D[] nodes = network.toArray();
		// place spheres
		int k = 0;
		for (Mesh mesh : spheres) {
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
		drawUniverse();
	}

	/**
	 * Adds the nodes, links, lights and camera to the scene.
	 */
	protected void drawUniverse() {
		Scene scene = graph3DScene.getScene();
		if (scene == null)
			return;
		scene.getChildren().clear();
		scene.add(new Object3D().add(spheres));
		if (graph3DCamera == null)
			graph3DCamera = network.getWorldView();
		scene.add(graph3DCamera);
		if (ambient == null)
			ambient = new AmbientLight(0x666666);
		scene.add(ambient);
		thothbot.parallax.core.shared.core.Geometry lines = network.getLinks();
		if (lines != null) {
			// this does not work - seems to be a bug (works but does not update the number
			// of line segments drawn)
			// ((thothbot.parallax.core.shared.core.Geometry)links.getGeometry()).setVertices(lines);
			// links.getGeometry().setVerticesNeedUpdate(true);
			// geometry can only be attached to one Canvas3d - clone geometry if not network
			// founder
			if (linkstyle == null) {
				linkstyle = new LineBasicMaterial();
				linkstyle.setOpacity(1.0);
				// XXX linewidth is ignored...
				linkstyle.setLinewidth(2);
			}
			linkstyle.setColor(geometry.isUndirected ? ColorMap3D.UNDIRECTED : ColorMap3D.DIRECTED);
			linkstyle.setVertexColors(Material.COLORS.VERTEX);
			Line links = new Line(lines, linkstyle, MODE.PIECES);
			links.setMatrixAutoUpdate(false);
			scene.add(links);
		}
		// // IMPORTANT: The following lines are crucial for Safari (desktop and
		// iPadOS). Safari
		// // requires special convincing to properly display lattices as well as
		// animated networks.
		// // Chrome, Firefox and Safari (iOS) are all fine. In those cases the
		// following lines are
		// // not needed but do no harm either.
		// if (network.isStatus(Status.NO_LAYOUT) ||
		// network.isStatus(Status.HAS_LAYOUT)) {
		// int k = 0;
		// for (Mesh sphere : spheres)
		// sphere.setMaterial(colors[k++]);
		// graph3DPanel.forceLayout();
		// }
	}

	@Override
	public void calcBounds(int width, int height) {
		Canvas3d canvas = graph3DScene.getCanvas();
		if (canvas == null)
			return;
		canvas.setSize(width, height);
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
	 * @return the canvas element
	 */
	public CanvasElement getCanvasElement() {
		graph3DScene.getRenderer().render(graph3DScene.getScene(), graph3DCamera);
		return graph3DScene.getCanvas().getCanvas();
	}

	/**
	 * Get the color of the node at index {@code node} as a CSS color string.
	 * 
	 * @param node the index of the node
	 * @return the color of the node
	 */
	public String getCSSColorAt(int node) {
		return "#" + data[node].getColor().getHexString();
	}

	/**
	 * Helper field for determining which node has been hit by mouse or tap.
	 */
	Raycaster raycaster = new Raycaster();

	/**
	 * Helper variable for additional effects on the 3D view. This handles anaglyph
	 * and stereo projections.
	 */
	private Effect effect = null;

	@Override
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

	@Override
	public void zoom(double zoom) {
		if (zoom <= 0.0)
			graph3DScene.zoom();
		else
			graph3DScene.zoom(1.0 / zoom);
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
	 * Set the projection of the camera. If {@code setOrtho == true} the camera uses
	 * an orthographic (parallel) projection and a perspective projection otherwise.
	 * 
	 * @param orthographic the flag to set an orthographic (parallel) projection for
	 *                     the camera
	 */
	public void setOrthographic(boolean orthographic) {
		graph3DScene.setOrtho(orthographic);
		drawUniverse();
		paint(true);
	}

	/**
	 * Check if the camera uses an orthographic projection.
	 * 
	 * @return {@code true} for orthographic projections
	 */
	public boolean isOrthographic() {
		return (graph3DCamera instanceof OrthographicCamera);
	}

	/**
	 * Set the anaglyph effect for the 3D view.
	 * 
	 * @param anaglyph {@code true} enable the anaglyph effect
	 */
	public void setAnaglyph(boolean anaglyph) {
		if (!anaglyph || isVR()) {
			graph3DPanel.getRenderer().deletePlugin(effect);
			effect = null;
		}
		if (anaglyph)
			effect = new Anaglyph(graph3DPanel.getRenderer(), graph3DScene.getScene());
	}

	/**
	 * Check if the graph is displayed as an anaglyph.
	 * 
	 * @return {@code true} if anaglyph shown
	 */
	public boolean isAnaglyph() {
		return (effect instanceof Anaglyph);
	}

	/**
	 * Set the stereo effect for the 3D view.
	 * 
	 * @param vr {@code true} to enable the stereo effect
	 */
	public void setVR(boolean vr) {
		if (!vr || isAnaglyph()) {
			graph3DPanel.getRenderer().deletePlugin(effect);
			effect = null;
		}
		if (vr)
			effect = new Stereo(graph3DPanel.getRenderer(), graph3DScene.getScene());
	}

	/**
	 * Check if the graph is displayed with stereo effect.
	 * 
	 * @return {@code true} if stereo effect shown
	 */
	public boolean isVR() {
		return (effect instanceof Stereo);
	}

	@Override
	public void export(MyContext2d ctx) {
		ctx.save();
		ctx.scale(scale, scale);
		ctx.drawImage(getCanvasElement(), 0, 0);
		ctx.restore();
	}

	/**
	 * The class for animating the 3D network structure.
	 */
	public class Pop3DScene extends AnimatedScene implements RequiresResize {

		/**
		 * Create a new 3D scene.
		 */
		public Pop3DScene() {
		}

		/**
		 * The control for rotating and zooming the scene.
		 */
		TrackballControls control;

		/**
		 * {@inheritDoc}
		 * <p>
		 * The scene should be defined already.
		 * 
		 * @see PopGraph3D#drawUniverse()
		 */
		@Override
		protected void onStart() {
			positionCamera();
		}

		/**
		 * Helper variable to fix unnecessary exceptions in parallax
		 * (renderingPanel is private in RenderingPanel class).
		 */
		RenderingPanel renderingPanel;

		@Override
		public WebGLRenderer getRenderer() {
			// prevent exceptions if rendering panel not yet initialized
			if (renderingPanel == null)
				return null;
			return super.getRenderer();
		}

		@Override
		public Canvas3d getCanvas() {
			// prevent exceptions if rendering panel not yet initialized
			if (renderingPanel == null)
				return null;
			return super.getCanvas();
		}
	
		@Override
		public void init(RenderingPanel renderingPanel, AnimationUpdateHandler animationUpdateHandler) {
			super.init(renderingPanel, animationUpdateHandler);
			this.renderingPanel = renderingPanel;
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
			if (newWorldView != null && graph3DCamera != null) {
				// copy position of camera
				graph3DCamera.setPosition(newWorldView.getPosition());
				graph3DCamera.setQuaternion(newWorldView.getQuaternion());
				graph3DCamera.setUp(newWorldView.getUp());
				graph3DCamera.setScale(newWorldView.getScale());
				graph3DCamera.setRotation(newWorldView.getRotation());
			}
			network.setWorldView(graph3DCamera);
		}

		/**
		 * Helper method to allocate, set, or change the camera for the scene.
		 * 
		 * @param doOrthographic {@code true} for an orthographic projection
		 */
		private void setOrtho(boolean doOrthographic) {
			Canvas3d c3d = getCanvas();
			int width = c3d.getWidth();
			int height = c3d.getHeight();
			if (width == 0 || height == 0)
				return;
			if (doOrthographic) {
				// orthographic projection
				if (graph3DCamera instanceof OrthographicCamera)
					return;
				graph3DCamera = new OrthographicCamera(width, height, 
						-10000, // near
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
			if (control == null)
				positionCamera();
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
			if (isActive) {
				control.update();
				getRenderer().render(getScene(), graph3DCamera);
				return;
			}
			stop();
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
	public void onResize() {
		super.onResize();
		graph3DScene.onResize();
		graph3DPanel.forceLayout();
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
		msgStyle.setFontSize(12.0 * 0.666 * getOffsetWidth() / g.measureText(msg).getWidth(), Unit.PX);
		g.restore();
		spheres.clear();
		return true;
	}

	@Override
	public void clearMessage() {
		if (hasMessage) {
			msgLabel.setVisible(false);
			hasMessage = false;
			graph3DScene.run();
		}
	}
}
