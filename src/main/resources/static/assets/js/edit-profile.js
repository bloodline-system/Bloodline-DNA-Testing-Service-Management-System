// File upload handling
            const fileInput = document.getElementById('file');
            const fileDisplay = document.getElementById('fileDisplay');
            const fileSelected = document.getElementById('fileSelected');
            const avatarPreview = document.getElementById('avatarPreview');
            const submitBtn = document.getElementById('submitBtn');

            fileInput.addEventListener('change', function(e) {
                const file = e.target.files[0];
                if (file) {
                    fileSelected.textContent = file.name;
                    fileSelected.style.display = 'block';

                    // Preview avatar
                    const reader = new FileReader();
                    reader.onload = function(e) {
                        avatarPreview.src = e.target.result;
                    };
                    reader.readAsDataURL(file);
                }
            });

            // Drag and drop
            fileDisplay.addEventListener('dragover', function(e) {
                e.preventDefault();
                fileDisplay.classList.add('dragover');
            });

            fileDisplay.addEventListener('dragleave', function(e) {
                e.preventDefault();
                fileDisplay.classList.remove('dragover');
            });

            fileDisplay.addEventListener('drop', function(e) {
                e.preventDefault();
                fileDisplay.classList.remove('dragover');

                const files = e.dataTransfer.files;
                if (files.length > 0) {
                    fileInput.files = files;
                    fileInput.dispatchEvent(new Event('change'));
                }
            });

            // Form submission loading state
            document.getElementById('profileForm').addEventListener('submit', function() {
                submitBtn.innerHTML = '<div class="loading-spinner"></div> Updating...';
                submitBtn.classList.add('btn-loading');
            });