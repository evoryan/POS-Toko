module.exports = {
  apps: [
    {
      name: 'pos-akbar-api',
      script: 'server.js',
      instances: 1, // Single instance to minimize memory on 2GB RAM server
      exec_mode: 'fork',
      autorestart: true,
      watch: false,
      max_memory_restart: '150M', // Automatically restarts if memory exceeds 150MB
      node_args: '--max-old-space-size=256', // Hard limit for V8 engine heap
      env: {
        NODE_ENV: 'production',
        PORT: 4750
      }
    }
  ]
};
