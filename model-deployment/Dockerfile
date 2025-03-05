# Use an official Python runtime as the base image
FROM python:3.9-slim

# Set the working directory in the container
WORKDIR /app

# Install dependencies
RUN pip install torch transformers

# Copy the downloaded model and tokenizer into the container
COPY fine-tuned-model /app/fine-tuned-model

# Copy the Python script to load and use the model
COPY server.py /app/server.py

# Command to run the script
CMD ["python", "server.py"]