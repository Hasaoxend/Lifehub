import { useEffect, useRef } from 'react';

interface Particle {
  x: number;
  y: number;
  size: number;
  speedX: number;
  speedY: number;
  opacity: number;
  hue: number;
}

export function InteractiveBackground() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const mousePosRef = useRef({ x: -100, y: -100 });
  const particlesRef = useRef<Particle[]>([]);
  const animationRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Set canvas size
    const resizeCanvas = () => {
      canvas.width = canvas.offsetWidth;
      canvas.height = canvas.offsetHeight;
      initParticles();
    };

    // Initialize particles
    const initParticles = () => {
      particlesRef.current = [];
      const particleCount = 100;
      
      for (let i = 0; i < particleCount; i++) {
        particlesRef.current.push({
          x: Math.random() * canvas.width,
          y: Math.random() * canvas.height,
          size: Math.random() * 4 + 3,
          speedX: (Math.random() - 0.5) * 0.8,
          speedY: (Math.random() - 0.5) * 0.8,
          opacity: Math.random() * 0.5 + 0.2,
          hue: Math.random() * 360 // Full rainbow
        });
      }
    };


    resizeCanvas();
    window.addEventListener('resize', resizeCanvas);

    // Smooth animation loop - NOT dependent on React state
    const animate = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      const mouse = mousePosRef.current;
      
      particlesRef.current.forEach((particle, index) => {
        // Calculate distance from mouse
        const dx = mouse.x - particle.x;
        const dy = mouse.y - particle.y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        
        // Smooth repel effect - gradual push
        if (distance < 120 && distance > 0) {
          const force = (120 - distance) / 120;
          particle.speedX -= (dx / distance) * force * 0.3;
          particle.speedY -= (dy / distance) * force * 0.3;
        }
        
        // Apply friction to slow down
        particle.speedX *= 0.98;
        particle.speedY *= 0.98;
        
        // Add gentle random drift
        particle.speedX += (Math.random() - 0.5) * 0.02;
        particle.speedY += (Math.random() - 0.5) * 0.02;
        
        // Limit speed
        const maxSpeed = 2;
        const speed = Math.sqrt(particle.speedX ** 2 + particle.speedY ** 2);
        if (speed > maxSpeed) {
          particle.speedX = (particle.speedX / speed) * maxSpeed;
          particle.speedY = (particle.speedY / speed) * maxSpeed;
        }
        
        // Move particle
        particle.x += particle.speedX;
        particle.y += particle.speedY;
        
        // Wrap around edges smoothly
        if (particle.x < -10) particle.x = canvas.width + 10;
        if (particle.x > canvas.width + 10) particle.x = -10;
        if (particle.y < -10) particle.y = canvas.height + 10;
        if (particle.y > canvas.height + 10) particle.y = -10;
        
        // Draw particle with glow
        ctx.save();
        ctx.shadowBlur = particle.size * 3;
        ctx.shadowColor = `hsla(${particle.hue}, 70%, 60%, 0.8)`;
        ctx.beginPath();
        ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2);
        ctx.fillStyle = `hsla(${particle.hue}, 70%, 65%, ${particle.opacity})`;
        ctx.fill();
        ctx.restore();
        
        // Draw connections to nearby particles
        for (let j = index + 1; j < particlesRef.current.length; j++) {
          const other = particlesRef.current[j];
          const odx = particle.x - other.x;
          const ody = particle.y - other.y;
          const odist = Math.sqrt(odx * odx + ody * ody);
          
          if (odist < 80) {
            ctx.beginPath();
            ctx.moveTo(particle.x, particle.y);
            ctx.lineTo(other.x, other.y);
            ctx.strokeStyle = `hsla(200, 50%, 60%, ${0.15 * (1 - odist / 80)})`;
            ctx.lineWidth = 0.5;
            ctx.stroke();
          }
        }
      });
      
      animationRef.current = requestAnimationFrame(animate);
    };
    
    animate();

    return () => {
      window.removeEventListener('resize', resizeCanvas);
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, []);

  const handleMouseMove = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (rect) {
      mousePosRef.current = {
        x: e.clientX - rect.left,
        y: e.clientY - rect.top
      };
    }
  };

  const handleMouseLeave = () => {
    mousePosRef.current = { x: -100, y: -100 };
  };

  const handleClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return;
    
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    
    // Add burst of particles on click
    for (let i = 0; i < 10; i++) {
      const angle = (Math.PI * 2 / 10) * i + Math.random() * 0.3;
      const speed = 3 + Math.random() * 2;
      particlesRef.current.push({
        x,
        y,
        size: Math.random() * 5 + 5,
        speedX: Math.cos(angle) * speed,
        speedY: Math.sin(angle) * speed,
        opacity: 0.8,
        hue: Math.random() * 360
      });
    }
    
    // Keep particle count reasonable
    if (particlesRef.current.length > 150) {
      particlesRef.current = particlesRef.current.slice(-120);
    }
  };

  return (
    <canvas
      ref={canvasRef}
      className="interactive-bg-canvas"
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      onClick={handleClick}
    />
  );
}
