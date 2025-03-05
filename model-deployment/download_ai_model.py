from transformers import AutoModelForCausalLM, AutoTokenizer
import torch

# Check if GPU is available
device = "cuda" if torch.cuda.is_available() else "cpu"
print(f"Using device: {device}")

# Model details
model_id = "FYPFAST/Llama-3.2-3B-Instruct-PEP8-Vulnerability-Python"

# Download and save the model and tokenizer
print("Downloading model and tokenizer...")
model = AutoModelForCausalLM.from_pretrained(model_id).to(device)
tokenizer = AutoTokenizer.from_pretrained(model_id)

# Save the model and tokenizer to a local directory
output_dir = "./fine-tuned-model"
print(f"Saving model and tokenizer to {output_dir}...")
model.save_pretrained(output_dir)
tokenizer.save_pretrained(output_dir)

print("Download and save completed!")