document.addEventListener('DOMContentLoaded', function() {
    const successMessage = document.querySelector('.alert-success');
    if (successMessage) {
        const highlightElements = document.querySelectorAll('.card-title, .current-price');
        highlightElements.forEach(function(element) {
            element.classList.add('highlight-animation');
            
            setTimeout(function() {
                element.classList.remove('highlight-animation');
            }, 2000);
        });
        
        setTimeout(function() {
            const closeButton = successMessage.querySelector('.btn-close');
            if (closeButton) {
                closeButton.click();
            } else {
                successMessage.style.display = 'none';
            }
        }, 5000);
    }
    
    if (!document.getElementById('highlight-animation-style')) {
        const style = document.createElement('style');
        style.id = 'highlight-animation-style';
        style.textContent = `
            @keyframes highlight {
                0% { background-color: transparent; }
                30% { background-color: rgba(40, 167, 69, 0.2); }
                100% { background-color: transparent; }
            }
            .highlight-animation {
                animation: highlight 2s ease-out;
            }
        `;
        document.head.appendChild(style);
    }
    
    const isNewlyCreated = new URLSearchParams(window.location.search).get('new') === 'true';
    if (isNewlyCreated) {
        const auctionDetailsCard = document.querySelector('.card');
        if (auctionDetailsCard) {
            auctionDetailsCard.classList.add('border-success');
            setTimeout(() => {
                auctionDetailsCard.classList.remove('border-success');
            }, 3000);
        }
    }
}); 