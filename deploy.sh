set -e
cd /var/www/apps/boardgames
git pull

cd backend
./mvnw flyway:migrate
./mvnw clean package
cp target/*.jar /opt/boardgames/app.jar
cd ..

cd frontend/react
npm ci
npm run bild

sudo systemctl restart boardgames