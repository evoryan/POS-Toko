FROM node:20-alpine

WORKDIR /app

# Copy package manifests and install production dependencies
COPY package*.json ./
RUN npm ci --only=production

# Copy application source code
COPY . .

EXPOSE 4750

USER node

CMD ["node", "server.js"]
