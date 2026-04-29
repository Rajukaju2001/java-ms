# 1. Kill any running java -jar processes
echo "Stopping all services..."
pkill -f 'java -jar'

# 2. Wait a moment for ports to clear
sleep 2

# 3. Start services silently and save logs to files
echo "Starting services fresh..."
for service in product-composite-service product-service recommendation-service review-service; do
  nohup java -jar microservices/$service/build/libs/*.jar > "$service.log" 2>&1 &
  echo "Launched $service"
done

echo "Done. Check *.log files if a service fails to start."