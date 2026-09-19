FROM caddy:2.10-alpine
COPY Caddyfile /etc/caddy/Caddyfile
# CI passes the already tested Vite output, with /api as its same-origin URL.
COPY dist/ /srv/
EXPOSE 80 443 8081
