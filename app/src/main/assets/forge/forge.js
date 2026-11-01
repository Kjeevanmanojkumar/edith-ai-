// 3D Forge Scene Engine
(function() {
    var container = document.getElementById('canvas-container');
    var canvas = document.createElement('canvas');
    var ctx = canvas.getContext('2d');
    container.appendChild(canvas);

    var width = window.innerWidth;
    var height = window.innerHeight;
    canvas.width = width;
    canvas.height = height;

    var state = {
        phase: 'BUILDING',
        progress: 5,
        stage: 'INITIALIZING'
    };

    var glyphs = ['{', '}', '<', '>', '/', '=', ';', '(', ')', '0', '1', '#', '$', '*', '&', '%'];
    var particles = [];
    var totalParticles = 180;

    for (var i = 0; i < totalParticles; i++) {
        particles.push({
            x: (Math.random() - 0.5) * width * 1.5,
            y: (Math.random() - 0.5) * height * 1.5,
            z: Math.random() * 800 + 100,
            glyph: glyphs[Math.floor(Math.random() * glyphs.length)],
            speed: Math.random() * 2 + 1,
            color: Math.random() > 0.5 ? '#FFB547' : '#3FE0D0'
        });
    }

    var rot = 0;
    var revealStart = 0;

    function render(time) {
        requestAnimationFrame(render);
        rot += 0.015;

        ctx.fillStyle = '#06050E';
        ctx.fillRect(0, 0, width, height);

        var cx = width / 2;
        var cy = height / 2;

        // Draw Floor Grid with expanding ripples
        ctx.strokeStyle = 'rgba(63, 224, 208, 0.12)';
        ctx.lineWidth = 1;
        var r = (state.progress / 100) * (width * 0.45);

        for (var ring = 1; ring <= 4; ring++) {
            ctx.beginPath();
            ctx.arc(cx, cy, (r * ring / 4) + (Math.sin(rot + ring) * 8), 0, Math.PI * 2);
            ctx.stroke();
        }

        // Reactor Core & Gyro Torus Rings
        var coreR = 50 + (state.progress * 0.35);

        // Ring 1 (Marigold)
        ctx.save();
        ctx.translate(cx, cy);
        ctx.rotate(rot);
        ctx.strokeStyle = '#FFB547';
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.ellipse(0, 0, coreR * 1.25, coreR * 0.45, 0, 0, Math.PI * 2);
        ctx.stroke();
        ctx.restore();

        // Ring 2 (Lagoon)
        ctx.save();
        ctx.translate(cx, cy);
        ctx.rotate(-rot * 1.4);
        ctx.strokeStyle = '#3FE0D0';
        ctx.lineWidth = 2.5;
        ctx.beginPath();
        ctx.ellipse(0, 0, coreR * 1.4, coreR * 0.55, Math.PI / 4, 0, Math.PI * 2);
        ctx.stroke();
        ctx.restore();

        // Ring 3 (Lotus)
        ctx.save();
        ctx.translate(cx, cy);
        ctx.rotate(rot * 0.8);
        ctx.strokeStyle = '#FF6FAE';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.ellipse(0, 0, coreR * 1.5, coreR * 0.6, -Math.PI / 4, 0, Math.PI * 2);
        ctx.stroke();
        ctx.restore();

        // Glowing Core
        var grad = ctx.createRadialGradient(cx, cy, 5, cx, cy, coreR);
        grad.addColorStop(0, '#FFFFFF');
        grad.addColorStop(0.4, '#FFB547');
        grad.addColorStop(0.8, '#FF8A3D');
        grad.addColorStop(1, 'rgba(255, 138, 61, 0)');
        ctx.fillStyle = grad;
        ctx.beginPath();
        ctx.arc(cx, cy, coreR, 0, Math.PI * 2);
        ctx.fill();

        // Instanced Glyph Particles spiraling toward core
        ctx.font = '12px monospace';
        for (var pIdx = 0; pIdx < particles.length; pIdx++) {
            var p = particles[pIdx];
            p.z -= p.speed * 4;
            if (p.z <= 10) {
                p.z = 800;
                p.x = (Math.random() - 0.5) * width * 1.5;
                p.y = (Math.random() - 0.5) * height * 1.5;
            }

            var scale = 300 / p.z;
            var px = cx + p.x * scale;
            var py = cy + p.y * scale;

            if (px > 0 && px < width && py > 0 && py < height) {
                ctx.fillStyle = p.color;
                ctx.globalAlpha = Math.min(1, scale * 1.5);
                ctx.fillText(p.glyph, px, py);
                ctx.globalAlpha = 1.0;
            }
        }
    }

    window.Forge = {
        init: function() {
            if (window.ForgeBridge && window.ForgeBridge.onReady) {
                window.ForgeBridge.onReady();
            }
        },
        updateState: function(jsonStr) {
            try {
                var s = JSON.parse(jsonStr);
                state.progress = s.progress || state.progress;
                state.stage = s.stage || state.stage;
                state.phase = s.phase || state.phase;
            } catch(e) {}
        },
        triggerReveal: function() {
            if (window.ForgeBridge && window.ForgeBridge.onRevealFinished) {
                setTimeout(function() {
                    window.ForgeBridge.onRevealFinished();
                }, 1200);
            }
        },
        dispose: function() {}
    };

    window.addEventListener('resize', function() {
        width = window.innerWidth;
        height = window.innerHeight;
        canvas.width = width;
        canvas.height = height;
    });

    requestAnimationFrame(render);
    setTimeout(function() {
        if (window.Forge.init) window.Forge.init();
    }, 200);
})();
