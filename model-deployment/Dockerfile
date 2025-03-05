# Stage 1: Base image with Python and dependencies
FROM python:3.9-slim as base

# Set the working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    git \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Stage 2: Download the model and tokenizer
FROM base as model_downloader

# Install transformers library
RUN pip install --no-cache-dir transformers

# Download and save the model and tokenizer
RUN python -c "from transformers import AutoModelForCausalLM, AutoTokenizer; \
model_id = 'FYPFAST/Llama-3.2-3B-Instruct-PEP8-Vulnerability-Python'; \
model = AutoModelForCausalLM.from_pretrained(model_id); \
tokenizer = AutoTokenizer.from_pretrained(model_id); \
model.save_pretrained('./fine-tuned-model'); \
tokenizer.save_pretrained('./fine-tuned-model')"

# Stage 3: Final image with the FastAPI app
FROM base as final

# Copy the downloaded model and tokenizer from the previous stage
COPY --from=model_downloader /app/fine-tuned-model /app/fine-tuned-model

# Copy the FastAPI application code
COPY . .

# Expose the port
EXPOSE 8000

# Command to run the FastAPI app
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]