/**
 * LayerSketch Studio - Sketchbook Edition
 * Featuring Autodesk Sketchbook style pencils (4H, 3H, 2H, H, F, HB, B, 2B, 3B, 4B, 5B, 6B, 7B, 8B, 9B)
 * with authentic graphite physics, pressure sensitivity, layers, and isolated layer recording.
 */

(function () {
  'use strict';

  // --- Utility Functions ---
  const uid = () => 'layer_' + Math.random().toString(36).substr(2, 9);
  const clamp = (val, min, max) => Math.min(Math.max(val, min), max);

  function interpolate(a, b, t) {
    return a + (b - a) * t;
  }

  // --- 15 AUTODESK SKETCHBOOK PENCIL GRADES ---
  const PENCIL_GRADES = {
    '4H': { name: '4H Pencil', hardness: 'Extra Hard', baseOpacity: 0.22, baseSize: 2.0, color: '#7a828e', grain: 0.05, desc: 'Very hard & light' },
    '3H': { name: '3H Pencil', hardness: 'Very Hard', baseOpacity: 0.28, baseSize: 2.5, color: '#6c7582', grain: 0.08, desc: 'Fine technical line' },
    '2H': { name: '2H Pencil', hardness: 'Hard', baseOpacity: 0.35, baseSize: 3.0, color: '#5e6774', grain: 0.12, desc: 'Light sketching' },
    'H':  { name: 'H Pencil', hardness: 'Medium Hard', baseOpacity: 0.42, baseSize: 3.5, color: '#505966', grain: 0.16, desc: 'Precision layout' },
    'F':  { name: 'F Pencil', hardness: 'Firm', baseOpacity: 0.50, baseSize: 4.0, color: '#444d5a', grain: 0.20, desc: 'Firm fine point' },
    'HB': { name: 'HB Pencil', hardness: 'Medium', baseOpacity: 0.60, baseSize: 4.5, color: '#38404d', grain: 0.25, desc: 'Standard sketch & write' },
    'B':  { name: 'B Pencil', hardness: 'Soft', baseOpacity: 0.68, baseSize: 5.0, color: '#2c333f', grain: 0.32, desc: 'Soft shading graphite' },
    '2B': { name: '2B Pencil', hardness: 'Soft & Dark', baseOpacity: 0.75, baseSize: 5.5, color: '#222833', grain: 0.40, desc: 'Classic art sketching' },
    '3B': { name: '3B Pencil', hardness: 'Very Soft', baseOpacity: 0.81, baseSize: 6.0, color: '#1a1f28', grain: 0.48, desc: 'Rich dark tone' },
    '4B': { name: '4B Pencil', hardness: 'Extra Soft', baseOpacity: 0.86, baseSize: 6.5, color: '#141820', grain: 0.56, desc: 'Soft velvety dark' },
    '5B': { name: '5B Pencil', hardness: 'Deep Dark', baseOpacity: 0.90, baseSize: 7.0, color: '#10131a', grain: 0.64, desc: 'Deep graphite shadow' },
    '6B': { name: '6B Pencil', hardness: 'Ultra Soft', baseOpacity: 0.94, baseSize: 7.5, color: '#0c0f15', grain: 0.72, desc: 'Heavy dark accents' },
    '7B': { name: '7B Pencil', hardness: 'Intense Soft', baseOpacity: 0.96, baseSize: 8.0, color: '#080a0e', grain: 0.80, desc: 'Smoky matte graphite' },
    '8B': { name: '8B Pencil', hardness: 'Heavy Graphite', baseOpacity: 0.98, baseSize: 8.5, color: '#050608', grain: 0.88, desc: 'Bold carbon shadow' },
    '9B': { name: '9B Pencil', hardness: 'Maximum Dark', baseOpacity: 1.0, baseSize: 9.0, color: '#020304', grain: 0.96, desc: 'Deepest matte black' }
  };

  // --- Application State ---
  const state = {
    project: {
      id: 'proj_' + Date.now(),
      name: 'Sketchbook Studio',
      width: 1080,
      height: 1080,
      fps: 30,
      duration: 5.0,
      background: '#ffffff', // Crisp white sketchbook paper default
      pages: []
    },
    currentPageIndex: 0,
    selectedLayerId: null,
    recordingTargetLayerId: null,
    activeTool: 'brush', // 'brush', 'eraser', 'select', 'pan', 'shape', 'text'
    activePencil: '2B',
    brush: {
      size: 5.5,
      opacity: 0.75,
      color: '#222833'
    },
    shape: {
      type: 'rectangle',
      fill: '#3b82f6',
      stroke: '#222833',
      strokeWidth: 2,
      radius: 12
    },
    text: {
      fontSize: 40,
      color: '#1e293b'
    },
    viewport: {
      zoom: 1,
      panX: 0,
      panY: 0,
      isPanning: false,
      startX: 0,
      startY: 0,
      showGrid: false,
      soloMode: false
    },
    timeline: {
      currentTime: 0,
      isPlaying: false,
      lastFrameTime: 0
    },
    recording: {
      isRecording: false,
      recorder: null,
      chunks: [],
      timerInterval: null
    },
    isDrawing: false,
    strokePoints: []
  };

  let mainCanvas, mainCtx, canvasStage, transformGizmo;
  let renderRequested = false;

  // --- Layer Factory ---
  function createLayer(type, options = {}) {
    const layer = {
      id: uid(),
      name: options.name || (type.charAt(0).toUpperCase() + type.slice(1) + ' Layer'),
      type: type, // 'drawing', 'text', 'shape', 'image'
      visible: true,
      locked: false,
      opacity: options.opacity !== undefined ? options.opacity : 1,
      blendMode: options.blendMode || 'source-over',
      x: options.x !== undefined ? options.x : 0,
      y: options.y !== undefined ? options.y : 0,
      width: options.width || state.project.width,
      height: options.height || state.project.height,
      scaleX: 1,
      scaleY: 1,
      rotation: 0,
      keyframes: options.keyframes || [],
      isRecTarget: false,
      textData: options.textData || { text: 'Sketchbook Studio', fontSize: 44, color: '#1e293b', bold: true, italic: false },
      shapeData: options.shapeData || { type: 'rectangle', fill: '#3b82f6', stroke: '#1e293b', strokeWidth: 2, radius: 12 },
      imageData: options.imageData || null,
      internalCanvas: null
    };

    if (type === 'drawing') {
      layer.internalCanvas = document.createElement('canvas');
      layer.internalCanvas.width = layer.width;
      layer.internalCanvas.height = layer.height;
    }

    return layer;
  }

  function getCurrentPage() {
    return state.project.pages[state.currentPageIndex] || null;
  }

  function getSelectedLayer() {
    const page = getCurrentPage();
    if (!page) return null;
    return page.layers.find(l => l.id === state.selectedLayerId) || null;
  }

  function getRecordingTargetLayer() {
    const page = getCurrentPage();
    if (!page) return null;
    return page.layers.find(l => l.id === state.recordingTargetLayerId) || getSelectedLayer();
  }

  // --- Keyframe Animation Calculator ---
  function getLayerAnimatedState(layer, time) {
    const base = {
      x: layer.x,
      y: layer.y,
      scaleX: layer.scaleX,
      scaleY: layer.scaleY,
      rotation: layer.rotation,
      opacity: layer.opacity
    };

    if (!layer.keyframes || layer.keyframes.length === 0) return base;

    const sorted = [...layer.keyframes].sort((a, b) => a.time - b.time);
    if (time <= sorted[0].time) return { ...base, ...sorted[0] };
    if (time >= sorted[sorted.length - 1].time) return { ...base, ...sorted[sorted.length - 1] };

    for (let i = 0; i < sorted.length - 1; i++) {
      const k1 = sorted[i];
      const k2 = sorted[i + 1];
      if (time >= k1.time && time <= k2.time) {
        const dur = k2.time - k1.time;
        const progress = dur > 0 ? (time - k1.time) / dur : 0;
        const t = 0.5 * (1 - Math.cos(progress * Math.PI)); // Cosine ease
        return {
          x: interpolate(k1.x ?? base.x, k2.x ?? base.x, t),
          y: interpolate(k1.y ?? base.y, k2.y ?? base.y, t),
          scaleX: interpolate(k1.scaleX ?? base.scaleX, k2.scaleX ?? base.scaleX, t),
          scaleY: interpolate(k1.scaleY ?? base.scaleY, k2.scaleY ?? base.scaleY, t),
          rotation: interpolate(k1.rotation ?? base.rotation, k2.rotation ?? base.rotation, t),
          opacity: interpolate(k1.opacity ?? base.opacity, k2.opacity ?? base.opacity, t)
        };
      }
    }
    return base;
  }

  // =========================================================================
  // ISOLATED LAYER RENDERING PIPELINE (CRITICAL REQUIREMENT)
  // Renders strictly ONE layer without rendering any other layers.
  // =========================================================================
  function renderSingleLayer(layer, time, ctx, boundsMode = 'canvas', options = {}) {
    if (!layer) return;

    const animState = getLayerAnimatedState(layer, time);

    ctx.save();
    if (boundsMode === 'layer') {
      const padding = options.padding || 0;
      ctx.translate(padding + layer.width / 2, padding + layer.height / 2);
      ctx.rotate((animState.rotation * Math.PI) / 180);
      ctx.scale(animState.scaleX, animState.scaleY);
      ctx.translate(-layer.width / 2, -layer.height / 2);
    } else {
      ctx.translate(animState.x + layer.width / 2, animState.y + layer.height / 2);
      ctx.rotate((animState.rotation * Math.PI) / 180);
      ctx.scale(animState.scaleX, animState.scaleY);
      ctx.translate(-layer.width / 2, -layer.height / 2);
    }

    ctx.globalAlpha = clamp(animState.opacity, 0, 1);
    ctx.globalCompositeOperation = layer.blendMode || 'source-over';

    if (layer.type === 'drawing' && layer.internalCanvas) {
      ctx.drawImage(layer.internalCanvas, 0, 0, layer.width, layer.height);
    } else if (layer.type === 'text') {
      const td = layer.textData || {};
      ctx.font = `${td.italic ? 'italic ' : ''}${td.bold ? 'bold ' : ''}${td.fontSize || 36}px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif`;
      ctx.fillStyle = td.color || '#1e293b';
      ctx.textBaseline = 'top';
      ctx.fillText(td.text || '', 0, 0);
    } else if (layer.type === 'shape') {
      const sd = layer.shapeData || {};
      ctx.fillStyle = sd.fill || '#3b82f6';
      ctx.strokeStyle = sd.stroke || '#1e293b';
      ctx.lineWidth = sd.strokeWidth || 0;
      ctx.beginPath();
      if (sd.type === 'rounded-rect') {
        const r = sd.radius || 12;
        ctx.moveTo(r, 0);
        ctx.arcTo(layer.width, 0, layer.width, layer.height, r);
        ctx.arcTo(layer.width, layer.height, 0, layer.height, r);
        ctx.arcTo(0, layer.height, 0, 0, r);
        ctx.arcTo(0, 0, layer.width, 0, r);
        ctx.closePath();
      } else if (sd.type === 'circle') {
        ctx.ellipse(layer.width / 2, layer.height / 2, layer.width / 2, layer.height / 2, 0, 0, Math.PI * 2);
      } else {
        ctx.rect(0, 0, layer.width, layer.height);
      }
      ctx.fill();
      if (sd.strokeWidth > 0) ctx.stroke();
    } else if (layer.type === 'image' && layer.imageData && layer.imageData.complete) {
      ctx.drawImage(layer.imageData, 0, 0, layer.width, layer.height);
    }

    ctx.restore();
  }

  // --- Full Composition Renderer ---
  function renderComposition(page, time, ctx, options = {}) {
    if (!page) return;

    if (state.project.background !== 'transparent') {
      ctx.fillStyle = state.project.background;
      ctx.fillRect(0, 0, state.project.width, state.project.height);
    }

    if (state.viewport.soloMode) {
      const soloLayer = getSelectedLayer();
      if (soloLayer) renderSingleLayer(soloLayer, time, ctx, 'canvas');
      return;
    }

    for (let i = page.layers.length - 1; i >= 0; i--) {
      const layer = page.layers[i];
      if (!layer.visible) continue;
      renderSingleLayer(layer, time, ctx, 'canvas');
    }

    if (state.viewport.showGrid && options.isMainDisplay) {
      ctx.save();
      ctx.strokeStyle = 'rgba(0, 0, 0, 0.08)';
      ctx.lineWidth = 1;
      for (let x = 50; x < state.project.width; x += 50) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, state.project.height);
        ctx.stroke();
      }
      for (let y = 50; y < state.project.height; y += 50) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(state.project.width, y);
        ctx.stroke();
      }
      ctx.restore();
    }
  }

  function requestRender() {
    if (!renderRequested) {
      renderRequested = true;
      requestAnimationFrame(performRender);
    }
  }

  function performRender() {
    renderRequested = false;
    if (!mainCanvas || !mainCtx) return;

    mainCtx.clearRect(0, 0, mainCanvas.width, mainCanvas.height);
    const page = getCurrentPage();
    if (page) {
      renderComposition(page, state.timeline.currentTime, mainCtx, { isMainDisplay: true });
    }

    updateTransformGizmo();
  }

  function updateTransformGizmo() {
    const selected = getSelectedLayer();
    if (!selected || !selected.visible || state.activeTool === 'pan' || state.activeTool === 'brush' || state.activeTool === 'eraser') {
      if (transformGizmo) transformGizmo.style.display = 'none';
      return;
    }

    const animState = getLayerAnimatedState(selected, state.timeline.currentTime);
    if (transformGizmo) {
      transformGizmo.style.display = 'block';
      transformGizmo.style.left = animState.x + 'px';
      transformGizmo.style.top = animState.y + 'px';
      transformGizmo.style.width = (selected.width * animState.scaleX) + 'px';
      transformGizmo.style.height = (selected.height * animState.scaleY) + 'px';
      transformGizmo.style.transform = `rotate(${animState.rotation}deg)`;
      transformGizmo.style.transformOrigin = 'center center';
    }
  }

  // --- Viewport Positioning (ROBUST FIX FOR BLACK SCREEN) ---
  function fitToScreen() {
    const container = document.getElementById('canvas-viewport-container');
    if (!container || !canvasStage) return;

    // Use container dimensions, fallback to window dimensions safely
    const w = container.clientWidth > 100 ? container.clientWidth : window.innerWidth;
    const h = container.clientHeight > 100 ? container.clientHeight : (window.innerHeight - 120);

    const padding = 36;
    const availW = Math.max(120, w - padding);
    const availH = Math.max(120, h - padding);

    const scale = Math.max(0.1, Math.min(availW / state.project.width, availH / state.project.height, 1.0));
    
    state.viewport.zoom = scale;
    state.viewport.panX = Math.round((w - state.project.width * scale) / 2);
    state.viewport.panY = Math.round((h - state.project.height * scale) / 2);

    applyStageTransform();
    requestRender();
  }

  function applyStageTransform() {
    if (!canvasStage) return;
    canvasStage.style.transform = `translate(${state.viewport.panX}px, ${state.viewport.panY}px) scale(${state.viewport.zoom})`;
    const zoomText = document.getElementById('zoom-level-text');
    if (zoomText) zoomText.textContent = Math.round(state.viewport.zoom * 100) + '%';
  }

  // --- Initial Sketchbook Demo Project Setup ---
  function initDemoProject() {
    const page = {
      id: 'page_1',
      name: 'Sketchbook Page 1',
      layers: []
    };

    // Layer 1: Sketchbook Paper Background
    const l1 = createLayer('shape', {
      name: 'Layer 1: Sketchbook Paper',
      x: 0,
      y: 0,
      width: 1080,
      height: 1080,
      shapeData: { type: 'rectangle', fill: '#ffffff', stroke: '#e2e8f0', strokeWidth: 2, radius: 0 }
    });

    // Layer 2: 2B Pencil Outline Sketch
    const l2 = createLayer('drawing', {
      name: 'Layer 2: 2B Pencil Figure Sketch',
      x: 0,
      y: 0,
      width: 1080,
      height: 1080
    });

    // Pre-draw authentic pencil sketch on Layer 2
    const ctx2 = l2.internalCanvas.getContext('2d');
    ctx2.lineCap = 'round';
    ctx2.lineJoin = 'round';
    ctx2.strokeStyle = '#222833';
    ctx2.lineWidth = 4;
    ctx2.globalAlpha = 0.75;

    // Draw artist portrait outline
    ctx2.beginPath();
    ctx2.arc(540, 420, 180, 0, Math.PI * 2); // Head oval
    ctx2.stroke();

    // Eyes & Smile
    ctx2.lineWidth = 3.5;
    ctx2.beginPath();
    ctx2.arc(480, 400, 22, 0, Math.PI * 2);
    ctx2.arc(600, 400, 22, 0, Math.PI * 2);
    ctx2.stroke();

    ctx2.beginPath();
    ctx2.arc(540, 470, 45, 0.2, Math.PI - 0.2); // Smile
    ctx2.stroke();

    // Shoulders
    ctx2.beginPath();
    ctx2.moveTo(320, 680);
    ctx2.quadraticCurveTo(540, 580, 760, 680);
    ctx2.stroke();

    // Layer 3: ANIMATED 6B GRAPHITE DETAIL (REC TARGET!)
    const l3 = createLayer('drawing', {
      name: 'Layer 3: Animated 6B Pencil (REC TARGET)',
      x: 0,
      y: 0,
      width: 1080,
      height: 1080,
      keyframes: [
        { time: 0.0, x: 0, y: 0, scaleX: 1.0, scaleY: 1.0, rotation: 0, opacity: 1 },
        { time: 2.5, x: 25, y: -20, scaleX: 1.08, scaleY: 1.08, rotation: 6, opacity: 1 },
        { time: 5.0, x: 0, y: 0, scaleX: 1.0, scaleY: 1.0, rotation: 0, opacity: 1 }
      ]
    });
    l3.isRecTarget = true;

    // Draw rich 6B graphite hair & shading on Layer 3
    const ctx3 = l3.internalCanvas.getContext('2d');
    ctx3.lineCap = 'round';
    ctx3.strokeStyle = '#0c0f15';
    ctx3.lineWidth = 6;
    ctx3.globalAlpha = 0.92;

    // Artistic hair curls
    for (let angle = 0; angle < Math.PI; angle += 0.2) {
      ctx3.beginPath();
      const hx = 540 + Math.cos(angle - Math.PI) * 200;
      const hy = 420 + Math.sin(angle - Math.PI) * 200;
      ctx3.moveTo(hx, hy);
      ctx3.quadraticCurveTo(hx + 30, hy - 60, hx + 10, hy - 110);
      ctx3.stroke();
    }

    // Layer 4: Title Typography
    const l4 = createLayer('text', {
      name: 'Layer 4: Title Typography',
      x: 240,
      y: 880,
      width: 600,
      height: 70,
      textData: { text: 'Sketchbook Studio 2B', fontSize: 44, color: '#1e293b', bold: true, italic: false }
    });

    page.layers = [l4, l3, l2, l1];
    state.project.pages.push(page);
    state.selectedLayerId = l3.id;
    state.recordingTargetLayerId = l3.id;
  }

  // --- Autodesk Sketchbook Realistic Pencil Physics Engine ---
  function initDrawingEvents() {
    const viewport = document.getElementById('canvas-viewport-container');
    if (!viewport) return;

    viewport.addEventListener('pointerdown', (e) => {
      if (state.activeTool === 'pan' || e.button === 1 || e.spaceKey) {
        state.viewport.isPanning = true;
        state.viewport.startX = e.clientX - state.viewport.panX;
        state.viewport.startY = e.clientY - state.viewport.panY;
        viewport.classList.add('panning');
        return;
      }

      if (state.activeTool === 'brush' || state.activeTool === 'eraser') {
        const selected = getSelectedLayer();
        if (!selected || selected.type !== 'drawing') {
          showToast('Select a Drawing Layer to sketch', 'info');
          return;
        }

        state.isDrawing = true;
        const pt = getCanvasCoordinates(e.clientX, e.clientY, selected);
        pt.pressure = (e.pressure && e.pressure > 0) ? e.pressure : 0.5;
        state.strokePoints = [pt];

        drawPencilDab(selected, pt);
        requestRender();
      }
    });

    window.addEventListener('pointermove', (e) => {
      if (state.viewport.isPanning) {
        state.viewport.panX = e.clientX - state.viewport.startX;
        state.viewport.panY = e.clientY - state.viewport.startY;
        applyStageTransform();
        return;
      }

      if (state.isDrawing) {
        const selected = getSelectedLayer();
        if (!selected || selected.type !== 'drawing') return;
        const pt = getCanvasCoordinates(e.clientX, e.clientY, selected);
        pt.pressure = (e.pressure && e.pressure > 0) ? e.pressure : 0.5;

        state.strokePoints.push(pt);
        if (state.strokePoints.length >= 2) {
          const p1 = state.strokePoints[state.strokePoints.length - 2];
          const p2 = state.strokePoints[state.strokePoints.length - 1];
          drawPencilSegment(selected, p1, p2);
          requestRender();
        }
      }
    });

    window.addEventListener('pointerup', () => {
      if (state.viewport.isPanning) {
        state.viewport.isPanning = false;
        viewport.classList.remove('panning');
      }
      if (state.isDrawing) {
        state.isDrawing = false;
        state.strokePoints = [];
      }
    });
  }

  function getCanvasCoordinates(clientX, clientY, layer) {
    const stageRect = canvasStage.getBoundingClientRect();
    const stageX = (clientX - stageRect.left) / state.viewport.zoom;
    const stageY = (clientY - stageRect.top) / state.viewport.zoom;
    return {
      x: stageX - layer.x,
      y: stageY - layer.y
    };
  }

  function drawPencilDab(layer, pt) {
    const ctx = layer.internalCanvas.getContext('2d');
    const pInfo = PENCIL_GRADES[state.activePencil] || PENCIL_GRADES['2B'];
    const pFactor = (pt.pressure || 0.5) * 1.4;

    ctx.save();
    if (state.activeTool === 'eraser') {
      ctx.globalCompositeOperation = 'destination-out';
      ctx.beginPath();
      ctx.arc(pt.x, pt.y, state.brush.size * 2, 0, Math.PI * 2);
      ctx.fill();
    } else {
      ctx.globalCompositeOperation = 'source-over';
      ctx.fillStyle = state.brush.color;
      ctx.globalAlpha = pInfo.baseOpacity * pFactor;
      ctx.beginPath();
      ctx.arc(pt.x, pt.y, (state.brush.size * pFactor) / 2, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.restore();
  }

  function drawPencilSegment(layer, p1, p2) {
    const ctx = layer.internalCanvas.getContext('2d');
    const pInfo = PENCIL_GRADES[state.activePencil] || PENCIL_GRADES['2B'];
    const pFactor = ((p1.pressure + p2.pressure) / 2) * 1.3;
    const radius = Math.max(1, (state.brush.size * pFactor) / 2);

    ctx.save();
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    if (state.activeTool === 'eraser') {
      ctx.globalCompositeOperation = 'destination-out';
      ctx.lineWidth = state.brush.size * 3;
      ctx.beginPath();
      ctx.moveTo(p1.x, p1.y);
      ctx.lineTo(p2.x, p2.y);
      ctx.stroke();
    } else {
      ctx.globalCompositeOperation = 'source-over';
      ctx.strokeStyle = state.brush.color;
      ctx.globalAlpha = clamp(pInfo.baseOpacity * pFactor, 0.05, 1.0);
      ctx.lineWidth = radius * 2;

      // Smooth stroke
      ctx.beginPath();
      ctx.moveTo(p1.x, p1.y);
      ctx.lineTo(p2.x, p2.y);
      ctx.stroke();

      // Authentic Graphite Grain Texture: micro particles along line
      if (pInfo.grain > 0.1) {
        ctx.fillStyle = state.brush.color;
        const dist = Math.hypot(p2.x - p1.x, p2.y - p1.y);
        const steps = Math.min(20, Math.ceil(dist / 4));
        for (let i = 0; i < steps; i++) {
          const t = i / steps;
          const gx = interpolate(p1.x, p2.x, t) + (Math.random() - 0.5) * radius * pInfo.grain * 2;
          const gy = interpolate(p1.y, p2.y, t) + (Math.random() - 0.5) * radius * pInfo.grain * 2;
          ctx.globalAlpha = Math.random() * pInfo.baseOpacity * 0.8;
          ctx.fillRect(gx, gy, 1.2, 1.2);
        }
      }
    }
    ctx.restore();
  }

  // --- Pencil Rack Selection Binding ---
  function selectPencil(grade) {
    if (!PENCIL_GRADES[grade]) return;
    state.activePencil = grade;
    const p = PENCIL_GRADES[grade];

    state.brush.size = p.baseSize;
    state.brush.opacity = p.baseOpacity;
    state.brush.color = p.color;

    // Update active chip
    document.querySelectorAll('.pencil-chip').forEach(c => {
      c.classList.toggle('active', c.getAttribute('data-pencil') === grade);
    });

    // Update puck label and dot
    const puckLbl = document.getElementById('puck-pencil-label');
    const puckDot = document.getElementById('puck-dot');
    if (puckLbl) puckLbl.textContent = grade;
    if (puckDot) {
      puckDot.style.background = p.color;
      puckDot.style.width = Math.min(24, Math.max(6, p.baseSize * 2)) + 'px';
      puckDot.style.height = puckDot.style.width;
    }

    state.activeTool = 'brush';
    document.querySelectorAll('.tool-btn').forEach(b => b.classList.remove('active'));
    document.querySelector('.tool-btn[data-tool="brush"]')?.classList.add('active');

    showToast(`${p.name} activated (${p.hardness})`, 'info');
  }

  // --- Sketchbook Puck Dragging (Adjust Size / Opacity) ---
  function initPuckEvents() {
    const puck = document.getElementById('sketchbook-puck');
    if (!puck) return;

    let isDraggingPuck = false;
    let startY = 0;
    let startSize = 5;

    puck.addEventListener('pointerdown', (e) => {
      isDraggingPuck = true;
      startY = e.clientY;
      startSize = state.brush.size;
      puck.setPointerCapture(e.pointerId);
    });

    puck.addEventListener('pointermove', (e) => {
      if (!isDraggingPuck) return;
      const deltaY = startY - e.clientY;
      state.brush.size = clamp(startSize + deltaY * 0.1, 1, 60);

      const puckDot = document.getElementById('puck-dot');
      if (puckDot) {
        const d = Math.min(28, Math.max(6, state.brush.size * 2));
        puckDot.style.width = d + 'px';
        puckDot.style.height = d + 'px';
      }
    });

    puck.addEventListener('pointerup', (e) => {
      if (isDraggingPuck) {
        isDraggingPuck = false;
        try { puck.releasePointerCapture(e.pointerId); } catch (_) {}
      }
    });

    // Click cycles through pencil grades (4H -> HB -> 2B -> 6B -> 9B)
    puck.addEventListener('click', (e) => {
      if (Math.abs(startY - e.clientY) < 5) {
        const cycle = ['4H', '2H', 'HB', '2B', '4B', '6B', '9B'];
        const curIdx = cycle.indexOf(state.activePencil);
        const next = cycle[(curIdx + 1) % cycle.length];
        selectPencil(next);
      }
    });
  }

  // --- UI Layer List & Controls ---
  function renderLayerList() {
    const list = document.getElementById('layer-list-container');
    if (!list) return;
    list.innerHTML = '';

    const page = getCurrentPage();
    if (!page) return;

    page.layers.forEach((layer) => {
      const row = document.createElement('div');
      row.className = 'layer-item-row';
      if (layer.id === state.selectedLayerId) row.classList.add('selected');
      if (layer.id === state.recordingTargetLayerId) row.classList.add('rec-target');

      // Visibility Eye Button
      const eyeBtn = document.createElement('button');
      eyeBtn.className = 'layer-btn-toggle' + (layer.visible ? ' active' : '');
      eyeBtn.innerHTML = layer.visible ? '👁️' : '🚫';
      eyeBtn.onclick = (e) => {
        e.stopPropagation();
        layer.visible = !layer.visible;
        renderLayerList();
        requestRender();
      };

      // Type Icon / Thumb
      const thumb = document.createElement('div');
      thumb.className = 'layer-thumb-preview';
      thumb.textContent = layer.type === 'drawing' ? '✏️' : (layer.type === 'text' ? 'T' : '⬟');

      // Info
      const info = document.createElement('div');
      info.className = 'layer-info-column';

      const title = document.createElement('div');
      title.className = 'layer-name-title';
      title.textContent = layer.name;

      const meta = document.createElement('div');
      meta.className = 'layer-meta-tag';
      meta.textContent = layer.type.toUpperCase();

      if (layer.id === state.recordingTargetLayerId) {
        const badge = document.createElement('span');
        badge.className = 'rec-target-badge';
        badge.innerHTML = `● REC TARGET`;
        meta.appendChild(badge);
      }

      info.appendChild(title);
      info.appendChild(meta);

      // Quick REC action
      const quickRecBtn = document.createElement('button');
      quickRecBtn.className = 'quick-rec-btn' + (layer.id === state.recordingTargetLayerId ? ' is-target' : '');
      quickRecBtn.innerHTML = `● REC`;
      quickRecBtn.onclick = (e) => {
        e.stopPropagation();
        setRecordingTarget(layer.id);
        openRecordingStudioModal();
      };

      row.onclick = () => {
        state.selectedLayerId = layer.id;
        renderLayerList();
        populateProperties();
        requestRender();
      };

      row.appendChild(eyeBtn);
      row.appendChild(thumb);
      row.appendChild(info);
      row.appendChild(quickRecBtn);
      list.appendChild(row);
    });

    updateRecTargetLabel();
  }

  function setRecordingTarget(layerId) {
    state.recordingTargetLayerId = layerId;
    const page = getCurrentPage();
    if (page) {
      page.layers.forEach(l => { l.isRecTarget = (l.id === layerId); });
    }
    updateRecTargetLabel();
    renderLayerList();
    showToast(`Target set to "${getRecordingTargetLayer()?.name}"`, 'info');
  }

  function updateRecTargetLabel() {
    const lbl = document.getElementById('rec-target-label');
    const target = getRecordingTargetLayer();
    if (lbl) {
      lbl.textContent = target ? target.name.slice(0, 14) : 'None';
    }
  }

  function populateProperties() {
    const layer = getSelectedLayer();
    if (!layer) return;

    const px = document.getElementById('prop-x');
    const py = document.getElementById('prop-y');
    const pw = document.getElementById('prop-w');
    const ph = document.getElementById('prop-h');
    const prot = document.getElementById('prop-rot');
    const pop = document.getElementById('prop-opacity');
    const pbm = document.getElementById('prop-blend-mode');

    if (px) px.value = Math.round(layer.x);
    if (py) py.value = Math.round(layer.y);
    if (pw) pw.value = Math.round(layer.width);
    if (ph) ph.value = Math.round(layer.height);
    if (prot) prot.value = Math.round(layer.rotation);
    if (pop) pop.value = Math.round(layer.opacity * 100);
    if (pbm) pbm.value = layer.blendMode || 'source-over';
  }

  // --- Timeline Engine ---
  let timelineAnimId = null;

  function initTimeline() {
    renderTimelineRuler();
    renderTimelineTracks();
  }

  function renderTimelineRuler() {
    const ruler = document.getElementById('timeline-ruler');
    if (!ruler) return;
    ruler.innerHTML = '';
    const totalSecs = state.project.duration;
    const pxPerSec = 90;
    ruler.style.width = (totalSecs * pxPerSec + 150) + 'px';

    for (let sec = 0; sec <= totalSecs; sec++) {
      const tick = document.createElement('div');
      tick.style.position = 'absolute';
      tick.style.left = (sec * pxPerSec) + 'px';
      tick.style.height = '100%';
      tick.style.borderLeft = '1px solid var(--border-subtle)';
      tick.style.paddingLeft = '3px';
      tick.style.fontSize = '9px';
      tick.style.color = 'var(--text-muted)';
      tick.textContent = sec + 's';
      ruler.appendChild(tick);
    }
  }

  function renderTimelineTracks() {
    const labelsCol = document.getElementById('timeline-labels-column');
    const tracksContainer = document.getElementById('timeline-tracks-container');
    if (!labelsCol || !tracksContainer) return;

    labelsCol.innerHTML = '';
    tracksContainer.innerHTML = '';

    const page = getCurrentPage();
    if (!page) return;

    const pxPerSec = 90;
    tracksContainer.style.width = (state.project.duration * pxPerSec + 150) + 'px';

    page.layers.forEach((layer) => {
      const labelRow = document.createElement('div');
      labelRow.className = 'track-label-row' + (layer.id === state.selectedLayerId ? ' selected' : '');
      labelRow.textContent = layer.name;
      labelRow.onclick = () => {
        state.selectedLayerId = layer.id;
        renderLayerList();
        renderTimelineTracks();
        populateProperties();
        requestRender();
      };
      labelsCol.appendChild(labelRow);

      const dataRow = document.createElement('div');
      dataRow.className = 'track-data-row';

      const bar = document.createElement('div');
      bar.className = 'track-bar';
      bar.style.left = '0px';
      bar.style.width = (state.project.duration * pxPerSec) + 'px';
      dataRow.appendChild(bar);

      if (layer.keyframes) {
        layer.keyframes.forEach((kf) => {
          const diamond = document.createElement('div');
          diamond.className = 'keyframe-diamond';
          diamond.style.left = (kf.time * pxPerSec) + 'px';
          diamond.title = `Keyframe at ${kf.time.toFixed(2)}s`;
          diamond.onclick = (e) => {
            e.stopPropagation();
            state.timeline.currentTime = kf.time;
            updatePlayhead();
            requestRender();
          };
          dataRow.appendChild(diamond);
        });
      }

      tracksContainer.appendChild(dataRow);
    });

    updatePlayhead();
  }

  function updatePlayhead() {
    const playhead = document.getElementById('timeline-playhead');
    const timecode = document.getElementById('timeline-timecode');
    const pxPerSec = 90;
    if (playhead) playhead.style.left = (state.timeline.currentTime * pxPerSec) + 'px';
    if (timecode) {
      const cur = state.timeline.currentTime.toFixed(2);
      const total = state.project.duration.toFixed(2);
      timecode.textContent = `00:${cur.padStart(5, '0')} / 00:${total.padStart(5, '0')}`;
    }
  }

  function togglePlayPause() {
    if (state.timeline.isPlaying) {
      pausePlayback();
    } else {
      startPlayback();
    }
  }

  function startPlayback() {
    state.timeline.isPlaying = true;
    state.timeline.lastFrameTime = performance.now();
    const btn = document.getElementById('btn-play-pause');
    if (btn) btn.innerHTML = `⏸️`;

    function loop(now) {
      if (!state.timeline.isPlaying) return;
      const delta = (now - state.timeline.lastFrameTime) / 1000;
      state.timeline.lastFrameTime = now;
      state.timeline.currentTime = (state.timeline.currentTime + delta) % state.project.duration;
      updatePlayhead();
      requestRender();
      timelineAnimId = requestAnimationFrame(loop);
    }
    timelineAnimId = requestAnimationFrame(loop);
  }

  function pausePlayback() {
    state.timeline.isPlaying = false;
    cancelAnimationFrame(timelineAnimId);
    const btn = document.getElementById('btn-play-pause');
    if (btn) btn.innerHTML = `▶`;
  }

  function stopPlayback() {
    pausePlayback();
    state.timeline.currentTime = 0;
    updatePlayhead();
    requestRender();
  }

  // =========================================================================
  // ISOLATED LAYER RECORDING STUDIO
  // =========================================================================
  let isolatedPreviewAnimId = null;

  function openRecordingStudioModal() {
    const modal = document.getElementById('recording-studio-modal');
    if (!modal) return;
    modal.classList.add('open');

    const select = document.getElementById('modal-rec-layer-select');
    select.innerHTML = '';
    const page = getCurrentPage();
    if (page) {
      page.layers.forEach((l) => {
        const opt = document.createElement('option');
        opt.value = l.id;
        opt.textContent = `${l.name} (${l.type})`;
        if (l.id === state.recordingTargetLayerId) opt.selected = true;
        select.appendChild(opt);
      });
      const fullOpt = document.createElement('option');
      fullOpt.value = 'FULL_COMPOSITION';
      fullOpt.textContent = '★ Full Composition (All Layers)';
      select.appendChild(fullOpt);
    }

    startIsolatedPreview();
  }

  function closeRecordingStudioModal() {
    const modal = document.getElementById('recording-studio-modal');
    if (modal) modal.classList.remove('open');
    cancelAnimationFrame(isolatedPreviewAnimId);
  }

  function startIsolatedPreview() {
    const previewCanvas = document.getElementById('isolated-preview-canvas');
    if (!previewCanvas) return;
    const previewCtx = previewCanvas.getContext('2d');

    const select = document.getElementById('modal-rec-layer-select');
    const boundsSelect = document.getElementById('modal-rec-bounds-select');
    const bgSelect = document.getElementById('modal-rec-bg-select');

    let simTime = 0;
    let lastT = performance.now();

    function previewLoop(now) {
      const delta = (now - lastT) / 1000;
      lastT = now;
      simTime = (simTime + delta) % state.project.duration;

      const targetId = select.value;
      const boundsMode = boundsSelect.value;
      const bg = bgSelect.value;

      previewCanvas.width = state.project.width;
      previewCanvas.height = state.project.height;

      previewCtx.clearRect(0, 0, previewCanvas.width, previewCanvas.height);
      if (bg !== 'transparent') {
        previewCtx.fillStyle = bg;
        previewCtx.fillRect(0, 0, previewCanvas.width, previewCanvas.height);
      }

      if (targetId === 'FULL_COMPOSITION') {
        renderComposition(getCurrentPage(), simTime, previewCtx);
      } else {
        const targetLayer = getCurrentPage()?.layers.find(l => l.id === targetId);
        if (targetLayer) renderSingleLayer(targetLayer, simTime, previewCtx, boundsMode);
      }

      isolatedPreviewAnimId = requestAnimationFrame(previewLoop);
    }

    cancelAnimationFrame(isolatedPreviewAnimId);
    isolatedPreviewAnimId = requestAnimationFrame(previewLoop);
  }

  async function startRecordingSession() {
    const select = document.getElementById('modal-rec-layer-select');
    const boundsMode = document.getElementById('modal-rec-bounds-select').value;
    const fps = parseInt(document.getElementById('modal-rec-fps-select').value, 10) || 30;
    const bg = document.getElementById('modal-rec-bg-select').value;
    const targetId = select.value;

    const page = getCurrentPage();
    const isFullComp = (targetId === 'FULL_COMPOSITION');
    const targetLayer = isFullComp ? null : page?.layers.find(l => l.id === targetId);

    const recCanvas = document.createElement('canvas');
    recCanvas.width = state.project.width;
    recCanvas.height = state.project.height;
    const recCtx = recCanvas.getContext('2d');

    if (!recCanvas.captureStream) {
      showToast('Canvas captureStream not supported in this WebView engine', 'error');
      return;
    }

    let mimeType = 'video/webm;codecs=vp9';
    if (!MediaRecorder.isTypeSupported(mimeType)) {
      mimeType = 'video/webm;codecs=vp8';
      if (!MediaRecorder.isTypeSupported(mimeType)) mimeType = 'video/webm';
    }

    const stream = recCanvas.captureStream(fps);
    let mediaRecorder;
    try {
      mediaRecorder = new MediaRecorder(stream, { mimeType });
    } catch (_) {
      mediaRecorder = new MediaRecorder(stream);
    }

    state.recording.chunks = [];
    state.recording.isRecording = true;

    mediaRecorder.ondataavailable = (e) => {
      if (e.data && e.data.size > 0) state.recording.chunks.push(e.data);
    };

    mediaRecorder.onstop = () => {
      state.recording.isRecording = false;
      clearInterval(state.recording.timerInterval);
      document.getElementById('recording-active-badge').style.display = 'none';

      const blob = new Blob(state.recording.chunks, { type: mimeType || 'video/webm' });
      showExportResultModal(blob, isFullComp ? 'Full_Composition' : targetLayer.name);
    };

    document.getElementById('recording-active-badge').style.display = 'flex';
    document.getElementById('btn-start-isolated-record').disabled = true;

    mediaRecorder.start(100);
    const totalDuration = state.project.duration;
    let recTime = 0;
    const frameInterval = 1000 / fps;
    const recStartTime = performance.now();

    state.recording.timerInterval = setInterval(() => {
      const elapsed = ((performance.now() - recStartTime) / 1000).toFixed(1);
      const timerEl = document.getElementById('recording-timer');
      if (timerEl) timerEl.textContent = `${elapsed}s / ${totalDuration}s`;
    }, 100);

    const recordInterval = setInterval(() => {
      recTime += frameInterval / 1000;
      recCtx.clearRect(0, 0, recCanvas.width, recCanvas.height);
      if (bg !== 'transparent') {
        recCtx.fillStyle = bg;
        recCtx.fillRect(0, 0, recCanvas.width, recCanvas.height);
      }

      if (isFullComp) {
        renderComposition(page, recTime % totalDuration, recCtx);
      } else {
        renderSingleLayer(targetLayer, recTime % totalDuration, recCtx, boundsMode);
      }

      if (recTime >= totalDuration) {
        clearInterval(recordInterval);
        mediaRecorder.stop();
        document.getElementById('btn-start-isolated-record').disabled = false;
        closeRecordingStudioModal();
      }
    }, frameInterval);

    showToast(`Recording ${isFullComp ? 'Composition' : targetLayer.name}...`, 'info');
  }

  function showExportResultModal(blob, layerName) {
    const modal = document.getElementById('export-result-modal');
    const player = document.getElementById('exported-video-player');
    const sizeLbl = document.getElementById('export-meta-size');
    const formatLbl = document.getElementById('export-meta-format');

    const videoUrl = URL.createObjectURL(blob);
    player.src = videoUrl;
    player.play().catch(() => {});

    const kb = (blob.size / 1024).toFixed(1);
    sizeLbl.textContent = `Size: ${kb} KB`;
    formatLbl.textContent = `Format: WebM Video`;
    modal.classList.add('open');

    document.getElementById('btn-export-download').onclick = () => {
      downloadBlob(blob, `${layerName.toLowerCase().replace(/[^a-z0-9]/g, '_')}_isolated.webm`);
    };

    document.getElementById('btn-export-share').onclick = () => {
      if (navigator.share) {
        const file = new File([blob], `${layerName}.webm`, { type: blob.type });
        navigator.share({ title: 'Layer Recording', files: [file] }).catch(() => {});
      } else if (window.AndroidBridge && window.AndroidBridge.shareFile) {
        window.AndroidBridge.shareFile(videoUrl, `${layerName}.webm`);
      } else {
        showToast('Sharing via browser download', 'info');
      }
    };
  }

  function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(url), 4000);
    showToast(`Saved ${filename}`, 'success');
  }

  function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.25s';
      setTimeout(() => container.removeChild(toast), 260);
    }, 2400);
  }

  // --- UI Bindings ---
  function initUIBindings() {
    // 15 Pencil Rack Click Listeners (4H to 9B)
    document.querySelectorAll('.pencil-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        const grade = chip.getAttribute('data-pencil');
        if (grade) selectPencil(grade);
      });
    });

    // Quick Color Input
    document.getElementById('quick-brush-color')?.addEventListener('input', (e) => {
      state.brush.color = e.target.value;
      const puckDot = document.getElementById('puck-dot');
      if (puckDot) puckDot.style.background = e.target.value;
    });

    // Left Tools
    document.querySelectorAll('.tool-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const tool = btn.getAttribute('data-tool');
        if (!tool) return;
        document.querySelectorAll('.tool-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        state.activeTool = tool;

        const viewport = document.getElementById('canvas-viewport-container');
        if (viewport) {
          viewport.className = (tool === 'brush' || tool === 'eraser') ? 'drawing' : (tool === 'pan' ? 'panning' : '');
        }
        requestRender();
      });
    });

    // Dock Tabs
    document.querySelectorAll('.dock-tab-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.dock-tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        const tab = btn.getAttribute('data-tab');
        document.getElementById('layers-tab-pane').style.display = tab === 'layers' ? 'flex' : 'none';
        document.getElementById('properties-tab-pane').style.display = tab === 'properties' ? 'flex' : 'none';
        if (tab === 'properties') populateProperties();
      });
    });

    // Layer Creation
    document.getElementById('btn-add-drawing-layer')?.addEventListener('click', () => {
      const page = getCurrentPage();
      if (!page) return;
      const l = createLayer('drawing', { name: `Sketch Layer ${page.layers.length + 1}` });
      page.layers.unshift(l);
      state.selectedLayerId = l.id;
      renderLayerList();
      renderTimelineTracks();
      requestRender();
      showToast('New drawing layer added', 'success');
    });

    document.getElementById('btn-add-text-layer')?.addEventListener('click', () => {
      const page = getCurrentPage();
      if (!page) return;
      const l = createLayer('text', { name: `Text Layer ${page.layers.length + 1}` });
      page.layers.unshift(l);
      state.selectedLayerId = l.id;
      renderLayerList();
      renderTimelineTracks();
      requestRender();
    });

    document.getElementById('btn-add-shape-layer')?.addEventListener('click', () => {
      const page = getCurrentPage();
      if (!page) return;
      const l = createLayer('shape', { name: `Shape ${page.layers.length + 1}` });
      page.layers.unshift(l);
      state.selectedLayerId = l.id;
      renderLayerList();
      renderTimelineTracks();
      requestRender();
    });

    // Set as Recording Target
    document.getElementById('btn-set-as-rec-target')?.addEventListener('click', () => {
      const sel = getSelectedLayer();
      if (sel) setRecordingTarget(sel.id);
    });

    // Clear Active Layer
    document.getElementById('btn-clear-canvas')?.addEventListener('click', () => {
      const sel = getSelectedLayer();
      if (sel && sel.type === 'drawing' && sel.internalCanvas) {
        sel.internalCanvas.getContext('2d').clearRect(0, 0, sel.width, sel.height);
        requestRender();
        showToast('Active sketch cleared', 'info');
      }
    });

    // Zoom & View
    document.getElementById('btn-zoom-fit')?.addEventListener('click', fitToScreen);
    document.getElementById('btn-toggle-grid')?.addEventListener('click', (e) => {
      state.viewport.showGrid = !state.viewport.showGrid;
      e.currentTarget.classList.toggle('active', state.viewport.showGrid);
      requestRender();
    });

    // Timeline Controls
    document.getElementById('btn-play-pause')?.addEventListener('click', togglePlayPause);
    document.getElementById('btn-stop-timeline')?.addEventListener('click', stopPlayback);

    // Recording Modals
    document.getElementById('btn-open-recorder')?.addEventListener('click', openRecordingStudioModal);
    document.getElementById('btn-quick-record')?.addEventListener('click', openRecordingStudioModal);
    document.getElementById('btn-timeline-rec-now')?.addEventListener('click', openRecordingStudioModal);
    document.getElementById('btn-close-recorder')?.addEventListener('click', closeRecordingStudioModal);
    document.getElementById('btn-cancel-recording-modal')?.addEventListener('click', closeRecordingStudioModal);
    document.getElementById('btn-start-isolated-record')?.addEventListener('click', startRecordingSession);

    document.getElementById('btn-close-export-result')?.addEventListener('click', () => {
      document.getElementById('export-result-modal').classList.remove('open');
    });

    // Export Dropdown
    const exportBtn = document.getElementById('btn-export-menu');
    const exportMenu = document.getElementById('export-dropdown-menu');
    exportBtn?.addEventListener('click', (e) => {
      e.stopPropagation();
      const rect = exportBtn.getBoundingClientRect();
      exportMenu.style.display = 'flex';
      exportMenu.style.top = (rect.bottom + 4) + 'px';
      exportMenu.style.left = Math.max(10, rect.left - 120) + 'px';
    });

    exportMenu?.addEventListener('click', (e) => {
      const item = e.target.closest('.context-menu-item');
      if (!item) return;
      const exp = item.getAttribute('data-export');
      if (exp === 'isolated-video') openRecordingStudioModal();
      else if (exp === 'full-video') {
        openRecordingStudioModal();
        document.getElementById('modal-rec-layer-select').value = 'FULL_COMPOSITION';
      } else if (exp === 'comp-png') {
        const c = document.createElement('canvas');
        c.width = state.project.width;
        c.height = state.project.height;
        renderComposition(getCurrentPage(), state.timeline.currentTime, c.getContext('2d'));
        c.toBlob(b => downloadBlob(b, 'sketchbook_artwork.png'), 'image/png');
      } else if (exp === 'layer-png') {
        const target = getRecordingTargetLayer();
        if (target) {
          const c = document.createElement('canvas');
          c.width = state.project.width;
          c.height = state.project.height;
          renderSingleLayer(target, state.timeline.currentTime, c.getContext('2d'), 'canvas');
          c.toBlob(b => downloadBlob(b, `${target.name}.png`), 'image/png');
        }
      }
      exportMenu.style.display = 'none';
    });

    document.addEventListener('click', () => {
      if (exportMenu) exportMenu.style.display = 'none';
    });

    // Window Resize
    window.addEventListener('resize', () => {
      fitToScreen();
    });
  }

  // --- App Initialization (Failsafe & Screen Blackout Prevention) ---
  function initApp() {
    try {
      mainCanvas = document.getElementById('main-display-canvas');
      if (!mainCanvas) return;

      mainCtx = mainCanvas.getContext('2d');
      canvasStage = document.getElementById('canvas-stage');
      transformGizmo = document.getElementById('transform-gizmo');

      mainCanvas.width = state.project.width;
      mainCanvas.height = state.project.height;
      if (canvasStage) {
        canvasStage.style.width = state.project.width + 'px';
        canvasStage.style.height = state.project.height + 'px';
      }

      initDemoProject();
      initUIBindings();
      initDrawingEvents();
      initPuckEvents();
      initTimeline();

      renderLayerList();
      populateProperties();

      // Immediate safe layout
      fitToScreen();
      // Secondary layout passes after WebView DOM measurements stabilize
      setTimeout(fitToScreen, 50);
      setTimeout(fitToScreen, 200);

      requestRender();
      showToast('Sketchbook Studio Ready: 15 Pencils (4H - 9B)', 'success');
    } catch (err) {
      console.error('Initialization error:', err);
    }
  }

  // Execute initialization regardless of load timing
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initApp);
  } else {
    initApp();
  }
  window.addEventListener('load', () => setTimeout(fitToScreen, 100));

})();
