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
import thothbot.parallax.core.client.shaders.Uniform;
import thothbot.parallax.core.shared.core.BufferAttribute;
import thothbot.parallax.core.shared.core.BufferGeometry;
import thothbot.parallax.core.shared.materials.Material;
import thothbot.parallax.core.shared.materials.ShaderMaterial;
import thothbot.parallax.core.shared.math.Color;
import thothbot.parallax.core.shared.math.Vector2;
import thothbot.parallax.core.shared.math.Vector3;
import thothbot.parallax.core.shared.objects.Mesh;

/**
 * Prototype renderer for thick 3D links. The implementation batches all links
 * into a single quad mesh and extrudes each segment in screen space with a
 * custom shader. This keeps the representation specific to EvoLudo's use case:
 * disjoint, independently colored link segments without line joins.
 *
 * @author Christoph Hauert
 */
public class Link3D extends Mesh {

	/**
	 * Number of triangle vertices used per link segment.
	 */
	private static final int VERTICES_PER_LINK = 6;

	/**
	 * Default line width in screen pixels.
	 */
	private static final double DEFAULT_LINE_WIDTH = 0.8;

	/**
	 * Default opacity for rendered links.
	 */
	private static final double DEFAULT_OPACITY = 0.8;

	/**
	 * Default viewport resolution before the first resize.
	 */
	private static final Vector2 DEFAULT_RESOLUTION = new Vector2(1.0, 1.0);

	/**
	 * Fallback color used when link geometry does not provide vertex colors.
	 */
	private static final Color DEFAULT_COLOR = new Color(0xffffff);

	/**
	 * Vertex positions required by the renderer.
	 */
	private BufferAttribute positionAttribute;

	/**
	 * Start point of each link segment.
	 */
	private BufferAttribute lineStartAttribute;

	/**
	 * End point of each link segment.
	 */
	private BufferAttribute lineEndAttribute;

	/**
	 * Side marker used for screen-space extrusion.
	 */
	private BufferAttribute sideAttribute;

	/**
	 * Marker selecting the segment start or end point.
	 */
	private BufferAttribute alongAttribute;

	/**
	 * Per-vertex colors used to support directional gradients.
	 */
	private BufferAttribute colorAttribute;

	/**
	 * Shader driving the thick-link rendering.
	 */
	private final LinkShader shader;

	/**
	 * Material wrapping the custom shader.
	 */
	private final ShaderMaterial shaderMaterial;

	/**
	 * Number of link segments stored in the current geometry.
	 */
	private int linkCount;

	/**
	 * Number of link segments the current buffers were allocated for.
	 */
	private int geometryLinkCount;

	/**
	 * Create an empty thick-link mesh.
	 */
	public Link3D() {
		super(new BufferGeometry(), new ShaderMaterial(new LinkShader()));
		shaderMaterial = (ShaderMaterial) getMaterial();
		shader = (LinkShader) shaderMaterial.getShader();
		shaderMaterial.setTransparent(true);
		shaderMaterial.setDepthWrite(false);
		shaderMaterial.setSide(Material.SIDE.DOUBLE);
		setFrustumCulled(false);
		setLineWidth(DEFAULT_LINE_WIDTH);
		setOpacity(DEFAULT_OPACITY);
		setResolution((int) DEFAULT_RESOLUTION.getX(), (int) DEFAULT_RESOLUTION.getY());
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
		if (lineGeometry == null) {
			clear();
			return;
		}
		List<Vector3> vertices = lineGeometry.getVertices();
		if (vertices == null || vertices.size() < 2) {
			clear();
			return;
		}
		List<Color> colors = lineGeometry.getColors();
		int nextLinkCount = vertices.size() / 2;
		ensureGeometry(nextLinkCount);
		linkCount = nextLinkCount;
		int vertexOffset = 0;
		for (int idx = 0; idx + 1 < vertices.size(); idx += 2) {
			Vector3 start = vertices.get(idx);
			Vector3 end = vertices.get(idx + 1);
			Color startColor = getVertexColor(colors, idx, fallbackColor);
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
	 * Hide all links from this mesh while keeping the allocated buffers intact. The
	 * reset path relies on reusing or replacing the mesh object rather than
	 * swapping in an empty geometry, because Parallax does not reliably recover the
	 * updated direct buffers after such a geometry reset.
	 */
	public void clear() {
		linkCount = 0;
		setVisible(false);
	}

	/**
	 * Set the viewport resolution used by the shader for screen-space extrusion.
	 *
	 * @param width  the viewport width in pixels
	 * @param height the viewport height in pixels
	 */
	public void setResolution(int width, int height) {
		double safeWidth = Math.max(1.0, width);
		double safeHeight = Math.max(1.0, height);
		shader.setResolution(safeWidth, safeHeight);
	}

	/**
	 * Set the thickness of the rendered links in pixels.
	 *
	 * @param lineWidth the desired line width
	 */
	public void setLineWidth(double lineWidth) {
		shader.setLineWidth(Math.max(1.0, lineWidth));
	}

	/**
	 * Set the opacity of the rendered links.
	 *
	 * @param opacity the desired opacity
	 */
	public void setOpacity(double opacity) {
		double alpha = Math.max(0.0, Math.min(1.0, opacity));
		shader.setOpacity(alpha);
		shaderMaterial.setOpacity(alpha);
		shaderMaterial.setTransparent(alpha < 1.0);
	}

	/**
	 * Get the number of links currently stored in the geometry.
	 *
	 * @return the number of rendered links
	 */
	public int getLinkCount() {
		return linkCount;
	}

	/**
	 * Allocate the buffer geometry used for the current link count.
	 *
	 * @param links the number of link segments
	 */
	private void createGeometry(int links) {
		int vertices = Math.max(0, links * VERTICES_PER_LINK);
		BufferGeometry geometry = new BufferGeometry();
		positionAttribute = createAttribute(vertices, 3);
		lineStartAttribute = createAttribute(vertices, 3);
		lineEndAttribute = createAttribute(vertices, 3);
		sideAttribute = createAttribute(vertices, 1);
		alongAttribute = createAttribute(vertices, 1);
		colorAttribute = createAttribute(vertices, 3);
		geometry.addAttribute("position", positionAttribute);
		geometry.addAttribute("lineStart", lineStartAttribute);
		geometry.addAttribute("lineEnd", lineEndAttribute);
		geometry.addAttribute("side", sideAttribute);
		geometry.addAttribute("along", alongAttribute);
		geometry.addAttribute("color", colorAttribute);
		setGeometry(geometry);
		geometryLinkCount = links;
	}

	/**
	 * Ensure that the backing geometry has capacity for the requested link count.
	 *
	 * @param links the number of link segments to render
	 */
	private void ensureGeometry(int links) {
		if (lineStartAttribute != null && lineEndAttribute != null && geometryLinkCount == links)
			return;
		createGeometry(links);
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
	 * Write one thick-link segment into the underlying buffers.
	 *
	 * @param vertexOffset the first vertex index of the segment
	 * @param start        the segment start position
	 * @param end          the segment end position
	 * @param startColor   the color at the start point
	 * @param endColor     the color at the end point
	 */
	private void writeLink(int vertexOffset, Vector3 start, Vector3 end, Color startColor, Color endColor) {
		writeVertex(vertexOffset, start, end, -1.0, 0.0, startColor);
		writeVertex(vertexOffset + 1, start, end, 1.0, 0.0, startColor);
		writeVertex(vertexOffset + 2, start, end, -1.0, 1.0, endColor);
		writeVertex(vertexOffset + 3, start, end, -1.0, 1.0, endColor);
		writeVertex(vertexOffset + 4, start, end, 1.0, 0.0, startColor);
		writeVertex(vertexOffset + 5, start, end, 1.0, 1.0, endColor);
	}

	/**
	 * Write one triangle vertex to the underlying buffers.
	 *
	 * @param index the vertex index
	 * @param start the segment start position
	 * @param end   the segment end position
	 * @param side  the extrusion side
	 * @param along the segment coordinate in {@code [0, 1]}
	 * @param color the vertex color
	 */
	private void writeVertex(int index, Vector3 start, Vector3 end, double side, double along, Color color) {
		Vector3 point = (along <= 0.0 ? start : end);
		positionAttribute.setXYZ(index, point.getX(), point.getY(), point.getZ());
		lineStartAttribute.setXYZ(index, start.getX(), start.getY(), start.getZ());
		lineEndAttribute.setXYZ(index, end.getX(), end.getY(), end.getZ());
		sideAttribute.setX(index, side);
		alongAttribute.setX(index, along);
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
		lineStartAttribute.setNeedsUpdate(true);
		lineEndAttribute.setNeedsUpdate(true);
		sideAttribute.setNeedsUpdate(true);
		alongAttribute.setNeedsUpdate(true);
		colorAttribute.setNeedsUpdate(true);
		((BufferGeometry) getGeometry()).computeBoundingBox();
		((BufferGeometry) getGeometry()).computeBoundingSphere();
	}

	/**
	 * Minimal shader for screen-space extrusion of link quads.
	 */
	private static class LinkShader extends Shader {

		/**
		 * Vertex shader source.
		 */
		private static final String VERTEX_SHADER = String.join("\n",
				"uniform vec2 resolution;",
				"uniform float linewidth;",
				"attribute vec3 lineStart;",
				"attribute vec3 lineEnd;",
				"attribute float side;",
				"attribute float along;",
				"attribute vec3 color;",
				"varying vec3 vColor;",
				"void main() {",
				"  vec4 startClip = projectionMatrix * modelViewMatrix * vec4(lineStart, 1.0);",
				"  vec4 endClip = projectionMatrix * modelViewMatrix * vec4(lineEnd, 1.0);",
				"  vec2 startNdc = startClip.xy / startClip.w;",
				"  vec2 endNdc = endClip.xy / endClip.w;",
				"  vec2 dir = endNdc - startNdc;",
				"  float dirLen = length(dir);",
				"  if (dirLen > 0.0) {",
				"    dir /= dirLen;",
				"  } else {",
				"    dir = vec2(1.0, 0.0);",
				"  }",
				"  vec2 normal = vec2(-dir.y, dir.x);",
				"  vec2 pixelOffset = normal * side * linewidth * 0.5;",
				"  vec2 clipOffset = vec2(2.0 * pixelOffset.x / resolution.x, 2.0 * pixelOffset.y / resolution.y);",
				"  vec4 clip = mix(startClip, endClip, along);",
				"  clip.xy += clipOffset * clip.w;",
				"  gl_Position = clip;",
				"  vColor = color;",
				"}");

		/**
		 * Fragment shader source.
		 */
		private static final String FRAGMENT_SHADER = String.join("\n",
				"uniform float opacity;",
				"varying vec3 vColor;",
				"void main() {",
				"  gl_FragColor = vec4(vColor, opacity);",
				"}");

		/**
		 * Create the shader.
		 */
		LinkShader() {
			super(VERTEX_SHADER, FRAGMENT_SHADER);
			addAttributes("lineStart", new Attribute(Attribute.TYPE.V3, null));
			addAttributes("lineEnd", new Attribute(Attribute.TYPE.V3, null));
			addAttributes("side", new Attribute(Attribute.TYPE.F, null));
			addAttributes("along", new Attribute(Attribute.TYPE.F, null));
		}

		@Override
		protected void initUniforms() {
			addUniform("resolution", new Uniform(Uniform.TYPE.V2, DEFAULT_RESOLUTION.clone()));
			addUniform("linewidth", new Uniform(Uniform.TYPE.F, DEFAULT_LINE_WIDTH));
			addUniform("opacity", new Uniform(Uniform.TYPE.F, DEFAULT_OPACITY));
		}

		/**
		 * Set the resolution uniform.
		 *
		 * @param width  the viewport width
		 * @param height the viewport height
		 */
		void setResolution(double width, double height) {
			getUniforms().get("resolution").setValue(new Vector2(width, height));
		}

		/**
		 * Set the line width uniform.
		 *
		 * @param lineWidth the desired width
		 */
		void setLineWidth(double lineWidth) {
			getUniforms().get("linewidth").setValue(lineWidth);
		}

		/**
		 * Set the opacity uniform.
		 *
		 * @param opacity the desired opacity
		 */
		void setOpacity(double opacity) {
			getUniforms().get("opacity").setValue(opacity);
		}
	}
}
