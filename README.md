# Custom Payment Flow

Follow these steps to get started for Node:
1. Make sure you have have node by running this following command `node --version` if it gave you an error this means you need to install it from the main [website](https://nodejs.org/).
2. Make sure you have git installed by running the following command `git -v` if it gave you and error you need to instll if from the main [website](https://git-scm.com/), then you need to watch a youtube video to set it up correctly.
3. Then fork this repository by clicking the link: [https://github.com/Bashamega/custompaymentflow/fork](https://github.com/Bashamega/custompaymentflow/fork)
4. Click the green code button, then coppy the HTTPS link that will appear.
5. Open a terminal and paste this command `git clone ` then paste the link that you coppeid then hit enter.
6. Rename the `.env.example` file to `.env` then open it in your vscode, the replace the `STRIPE_PUBLISHABLE_KEY` and `STRIPE_SECRET_KEY` value with the kays that you have.
7. Finally open the terminal again and run these commands:
```
cd server/node # there's a README in this folder with instructions
npm install
npm start
```
Then start coding but don't close the terminals window since it is running the server.
And don't forget to hit the star button on this repository :)
