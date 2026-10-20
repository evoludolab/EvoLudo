//
// EvoLudo Project
//
// Copyright 2010-2026 Christoph Hauert
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

import java.util.List;

import org.evoludo.simulator.ColorMap3D;

import thothbot.parallax.core.client.gl2.arrays.Float32Array;
import thothbot.parallax.core.client.shaders.Attribute;
import thothbot.parallax.core.client.shaders.Shader;
import thothbot.parallax.core.shared.core.BufferAttribute;
import thothbot.parallax.core.shared.core.BufferGeometry;
import thothbot.parallax.core.shared.materials.Material;
import thothbot.parallax.core.shared.materials.ShaderMaterial;
import thothbot.parallax.core.shared.math.Color;
import thothbot.parallax.core.shared.math.Vector3;
import thothbot.parallax.core.shared.objects.Mesh;

/**
 * Renderer for thick 3D links. Each link segment is expanded into a small open
 * prism in world coordinates, which gives links a proper depth relationship to
 * spheres and to other links while keeping the appearance unlit and simple.
 *
 * @author Christoph Hauert
 */
public class Link3D extends Mesh {

	/**
	 * Number of rectangular side faces per link prism.
	 */
	private static final int FACES_PER_LINK = 4;

	/**
	 * Number of triangle vertices used per rectangular side face.
	 */
	private static final int VERTICES_PER_FACE = 6;

	/**
	 * Number of triangle vertices used per link segment.
	 */
	private static final int VERTICES_PER_LINK = FACES_PER_LINK * VERTICES_PER_FACE;

	/**
	 * Smallest admissible link width in world coordinates.
	 */
	private static final double MIN_LINK_WIDTH = 0.05;

	/**
	 * Smallest admissible squared link length.
	 */
	private static final double MIN_AXIS_LENGTH2 = 1.0e-8;

	/**
	 * Threshold used when choosing a helper axis for constructing the prism basis.
	 */
	private static final double HELPER_AXIS_Z_THRESHOLD = 0.9;

	/**
	 * Fallback color used when link geometry does not provide vertex colors.
	 */
	private static final Color DEFAULT_COLOR = new Color(0xffffff);

	/**
	 * Vertex positions required by the renderer.
	 */
	private BufferAttribute positionAttribute;

	/**
	 * Per-vertex colors used to support directional gradients.
	 */
	private BufferAttribute colorAttribute;

	/**
	 * Material wrapping the custom shader.
	 */
	private final ShaderMaterial shaderMaterial;

	/**
	 * Temporary storage for the link axis.
	 */
	private final Vector3 axis = new Vector3();

	/**
	 * Temporary helper axis used to construct an orthonormal basis around the
	 * link axis.
	 */
	private final Vector3 axisHelper = new Vector3();

	/**
	 * Temporary first basis vector spanning the prism cross-section.
	 */
	private final Vector3 radialU = new Vector3();

	/**
	 * Temporary second basis vector spanning the prism cross-section.
	 */
	private final Vector3 radialV = new Vector3();

	/**
	 * Number of link segments stored in the current geometry.
	 */
	private int linkCount;

	/**
	 * Number of link segments the current buffers were allocated for.
	 */
	private int geometryLinkCount;

	/**
	 * Current link width in world coordinates.
	 */
	private double lineWidth = MIN_LINK_WIDTH;

	/**
	 * Source line geometry used to rebuild thick-link prisms after width changes.
	 */
	private thothbot.parallax.core.shared.core.Geometry linkGeometry;

	/**
	 * Fallback color used when rebuilding links without explicit vertex colors.
	 */
	private Color linkFallbackColor = DEFAULT_COLOR;

	/**
	 * Create an empty 3D link mesh.
	 */
	public Link3D() {
		super(new BufferGeometry(), new ShaderMaterial(new LinkShader()));
		shaderMaterial = (ShaderMaterial) getMaterial();
		shaderMaterial.setTransparent(false);
		shaderMaterial.setDepthWrite(true);
		shaderMaterial.setOpacity(1.0);
		shaderMaterial.setSide(Material.SIDE.DOUBLE);
		setFrustumCulled(false);
	}

	/**
	 * Update the links rendered by this mesh.
	 *
	 * @param lineGeometry the geometry containing link endpoints and optional
	 *                     colors
	 */
	public void setLinks(thothbot.parallax.core.shared.core.Geometry lineGeometry) {
		setLinks(lineGeometry, DEFAULT_COLOR);
	}

	/**
	 * Update the links rendered by this mesh.
	 *
	 * @param lineGeometry  the geometry containing link endpoints and optional
	 *                      colors
	 * @param fallbackColor the color to use when vertex colors are absent
	 */
	public void setLinks(thothbot.parallax.core.shared.core.Geometry lineGeometry, Color fallbackColor) {
		linkGeometry = lineGeometry;
		linkFallbackColor = fallbackColor;
		rebuildLinks();
	}

	/**
	 * Hide all links from this mesh while keeping the allocated buffers intact.
	 */
	public void clear() {
		linkGeometry = null;
		linkFallbackColor = DEFAULT_COLOR;
		linkCount = 0;
		setVisible(false);
	}

	/**
	 * Set the thickness of the rendered links in world coordinates.
	 *
	 * @param lineWidth the desired link width
	 */
	public void setLineWidth(double lineWidth) {
		double safeWidth = Math.max(MIN_LINK_WIDTH, lineWidth);
		if (Math.abs(this.lineWidth - safeWidth) < 1.0e-12)
			return;
		this.lineWidth = safeWidth;
		if (linkGeometry != null)
			rebuildLinks();
	}

	/**
	 * Rebuild the prism geometry from the cached source lines and current width.
	 */
	private void rebuildLinks() {
		if (linkGeometry == null) {
			clear();
			return;
		}
		List<Vector3> vertices = linkGeometry.getVertices();
		if (vertices == null || vertices.size() < 2) {
			clear();
			return;
		}
		List<Color> colors = linkGeometry.getColors();
		int nextLinkCount = vertices.size() / 2;
		ensureGeometry(nextLinkCount);
		linkCount = nextLinkCount;
		int vertexOffset = 0;
		for (int idx = 0; idx + 1 < vertices.size(); idx += 2) {
			Vector3 start = vertices.get(idx);
			Vector3 end = vertices.get(idx + 1);
			Color startColor = getVertexColor(colors, idx, linkFallbackColor);
			Color endColor = getVertexColor(colors, idx + 1, startColor);
			if (!startColor.equals(endColor)) {
				startColor = ColorMap3D.DIRECTED_SRC;
				endColor = ColorMap3D.DIRECTED_DST;
			}
			writeLink(vertexOffset, start, end, startColor, endColor);
			vertexOffset += VERTICES_PER_LINK;
		}
		markGeometryDirty();
		setVisible(linkCount > 0);
	}

	/**
	 * Allocate or resize the buffer geometry used for the current link count while
	 * keeping the mesh and geometry instances stable.
	 *
	 * @param links the number of link segments
	 */
	private void createGeometry(int links) {
		int vertices = Math.max(0, links * VERTICES_PER_LINK);
		if (positionAttribute == null || colorAttribute == null) {
			BufferGeometry geometry = (BufferGeometry) getGeometry();
			positionAttribute = createAttribute(vertices, 3);
			colorAttribute = createAttribute(vertices, 3);
			geometry.addAttribute("position", positionAttribute);
			geometry.addAttribute("color", colorAttribute);
		} else {
			positionAttribute.setArray(Float32Array.create(vertices * 3));
			positionAttribute.setNumItems(vertices);
			colorAttribute.setArray(Float32Array.create(vertices * 3));
			colorAttribute.setNumItems(vertices);
		}
		geometryLinkCount = links;
		invalidateGpuBuffers();
	}

	/**
	 * Ensure that the backing geometry has capacity for the requested link count.
	 *
	 * @param links the number of link segments to render
	 */
	private void ensureGeometry(int links) {
		if (positionAttribute != null && geometryLinkCount == links) {
			invalidateGpuBuffers();
			return;
		}
		createGeometry(links);
	}

	/**
	 * Force the renderer to upload fresh GPU buffers for the current attributes.
	 * Parallax caches WebGL buffers behind each attribute, so dynamic link updates
	 * must invalidate those handles explicitly.
	 */
	private void invalidateGpuBuffers() {
		positionAttribute.setBuffer(null);
		positionAttribute.setNeedsUpdate(true);
		colorAttribute.setBuffer(null);
		colorAttribute.setNeedsUpdate(true);
	}

	/**
	 * Create a floating-point buffer attribute.
	 *
	 * @param vertices the number of vertices
	 * @param itemSize the number of components per vertex
	 * @return the new attribute
	 */
	private BufferAttribute createAttribute(int vertices, int itemSize) {
		BufferAttribute attribute = new BufferAttribute(Float32Array.create(vertices * itemSize), itemSize);
		attribute.setNumItems(vertices);
		return attribute;
	}

	/**
	 * Write one thick-link prism into the underlying buffers.
	 *
	 * @param vertexOffset the first vertex index of the segment
	 * @param start        the segment start position
	 * @param end          the segment end position
	 * @param startColor   the color at the start point
	 * @param endColor     the color at the end point
	 */
	private void writeLink(int vertexOffset, Vector3 start, Vector3 end, Color startColor, Color endColor) {
		axis.sub(end, start);
		if (axis.lengthSq() < MIN_AXIS_LENGTH2) {
			double x = start.getX();
			double y = start.getY();
			double z = start.getZ();
			for (int idx = 0; idx < VERTICES_PER_LINK; idx++)
				writeVertex(vertexOffset + idx, x, y, z, startColor);
			return;
		}
		axis.normalize();
		if (Math.abs(axis.getZ()) < HELPER_AXIS_Z_THRESHOLD)
			axisHelper.set(0.0, 0.0, 1.0);
		else
			axisHelper.set(0.0, 1.0, 0.0);
		radialU.cross(axis, axisHelper).normalize().multiply(0.5 * lineWidth);
		radialV.cross(axis, radialU).normalize().multiply(0.5 * lineWidth);
		int offset = vertexOffset;
		offset = writeFace(offset, start, end, startColor, endColor, 1.0, 1.0, -1.0, 1.0);
		offset = writeFace(offset, start, end, startColor, endColor, -1.0, 1.0, -1.0, -1.0);
		offset = writeFace(offset, start, end, startColor, endColor, -1.0, -1.0, 1.0, -1.0);
		writeFace(offset, start, end, startColor, endColor, 1.0, -1.0, 1.0, 1.0);
	}

	/**
	 * Write one rectangular side face of the link prism.
	 *
	 * @param vertexOffset the first vertex index of the face
	 * @param start        the segment start position
	 * @param end          the segment end position
	 * @param startColor   the color at the start point
	 * @param endColor     the color at the end point
	 * @param uA           the first corner coefficient along {@link #radialU}
	 * @param vA           the first corner coefficient along {@link #radialV}
	 * @param uB           the second corner coefficient along {@link #radialU}
	 * @param vB           the second corner coefficient along {@link #radialV}
	 * @return the next free vertex index
	 */
	private int writeFace(int vertexOffset, Vector3 start, Vector3 end, Color startColor, Color endColor, double uA,
			double vA, double uB, double vB) {
		double oxA = uA * radialU.getX() + vA * radialV.getX();
		double oyA = uA * radialU.getY() + vA * radialV.getY();
		double ozA = uA * radialU.getZ() + vA * radialV.getZ();
		double oxB = uB * radialU.getX() + vB * radialV.getX();
		double oyB = uB * radialU.getY() + vB * radialV.getY();
		double ozB = uB * radialU.getZ() + vB * radialV.getZ();
		double sxA = start.getX() + oxA;
		double syA = start.getY() + oyA;
		double szA = start.getZ() + ozA;
		double exA = end.getX() + oxA;
		double eyA = end.getY() + oyA;
		double ezA = end.getZ() + ozA;
		double sxB = start.getX() + oxB;
		double syB = start.getY() + oyB;
		double szB = start.getZ() + ozB;
		double exB = end.getX() + oxB;
		double eyB = end.getY() + oyB;
		double ezB = end.getZ() + ozB;
		writeVertex(vertexOffset, sxA, syA, szA, startColor);
		writeVertex(vertexOffset + 1, sxB, syB, szB, startColor);
		writeVertex(vertexOffset + 2, exB, eyB, ezB, endColor);
		writeVertex(vertexOffset + 3, sxA, syA, szA, startColor);
		writeVertex(vertexOffset + 4, exB, eyB, ezB, endColor);
		writeVertex(vertexOffset + 5, exA, eyA, ezA, endColor);
		return vertexOffset + VERTICES_PER_FACE;
	}

	/**
	 * Write one triangle vertex to the underlying buffers.
	 *
	 * @param index the vertex index
	 * @param x     the x-coordinate
	 * @param y     the y-coordinate
	 * @param z     the z-coordinate
	 * @param color the vertex color
	 */
	private void writeVertex(int index, double x, double y, double z, Color color) {
		positionAttribute.setXYZ(index, x, y, z);
		colorAttribute.setXYZ(index, color.getR(), color.getG(), color.getB());
	}

	/**
	 * Resolve the color for one line vertex.
	 *
	 * @param colors        the optional color array
	 * @param index         the vertex index
	 * @param fallbackColor the color to use when no explicit value exists
	 * @return the selected color
	 */
	private Color getVertexColor(List<Color> colors, int index, Color fallbackColor) {
		if (colors == null || index >= colors.size())
			return fallbackColor;
		return colors.get(index);
	}

	/**
	 * Mark all geometry buffers as dirty after an update.
	 */
	private void markGeometryDirty() {
		positionAttribute.setNeedsUpdate(true);
		colorAttribute.setNeedsUpdate(true);
		((BufferGeometry) getGeometry()).computeBoundingBox();
		((BufferGeometry) getGeometry()).computeBoundingSphere();
	}

	/**
	 * Minimal unlit shader for world-space link prisms.
	 */
	private static class LinkShader extends Shader {

		/**
		 * Vertex shader source.
		 */
		private static final String VERTEX_SHADER = String.join("\n",
				"attribute vec3 color;",
				"varying vec3 vColor;",
				"void main() {",
				"  gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);",
				"  vColor = color;",
				"}");

		/**
		 * Fragment shader source.
		 */
		private static final String FRAGMENT_SHADER = String.join("\n",
				"varying vec3 vColor;",
				"void main() {",
				"  gl_FragColor = vec4(vColor, 1.0);",
				"}");

		/**
		 * Create the shader.
		 */
		LinkShader() {
			super(VERTEX_SHADER, FRAGMENT_SHADER);
			addAttributes("color", new Attribute(Attribute.TYPE.V3, null));
		}

		/**
		 * Initialize shader uniforms.
		 */
		@Override
		protected void initUniforms() {
			// This shader is fully driven by vertex attributes and built-in matrices.
		}
	}
}
